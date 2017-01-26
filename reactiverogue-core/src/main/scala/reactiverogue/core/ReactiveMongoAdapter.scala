package reactiverogue.core

import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.bson.BSONFindAndModifyCommand
import reactivemongo.api.commands.{DefaultWriteResult, WriteConcern, WriteResult}
import reactivemongo.bson._
import reactivemongo.api._
import reactivemongo.core.commands._
import reactivemongo.play.iteratees._

import scala.concurrent.{ExecutionContext, Future}
import reactivemongo.api.collections.GenericQueryBuilder
import reactiverogue.db.MongoResolution

class ReactiveMongoAdapter[MB] {

  import QueryHelpers._
  import MongoHelpers.MongoBuilder._

  val EmptyResult =
    LastError(ok = true,
              err = None,
              code = None,
              errMsg = None,
              originalDocument = None,
              updated = 0,
              updatedExisting = false)

  private def queryCollection(query: Query[_, _, _])(
      implicit res: MongoResolution,
      ec: ExecutionContext): Future[BSONCollection] = {
    res.database.map(_.apply(query.collectionName))
  }

  //TODO: make it looking for async commands
  private[reactiverogue] def runCommand[M <: MB, T](description: => String, query: Query[M, _, _])(
      f: => T): T = {
    // Use nanoTime instead of currentTimeMillis to time the query since
    // currentTimeMillis only has 10ms granularity on many systems.
    val start = System.nanoTime
    try {
      logger.onExecuteQuery(query, description, f)
    } catch {
      case e: Exception =>
        throw new RogueException(
          "Mongo query [%s] failed after %d ms".format(description,
                                                       (System.nanoTime - start) / (1000 * 1000)),
          e)
    } finally {
      logger.log(query, description, (System.nanoTime - start) / (1000 * 1000))
    }
  }

  def count[M <: MB](query: Query[M, _, _])(implicit ec: ExecutionContext,
                                            res: MongoResolution): Future[Int] = {
    val queryClause = transformer.transformQuery(query)
    validator.validateQuery(queryClause)
    val condition: BSONDocument = buildCondition(queryClause.condition)
    val description: String = buildConditionString("count", query.collectionName, queryClause)

    runCommand(description, queryClause) {
      queryCollection(query) flatMap (_.count(selector = Some(condition),
                                              limit = query.lim.getOrElse(0),
                                              skip = query.sk.getOrElse(0)))
    }
  }

  //  def countDistinct[M <: MB](query: Query[M, _, _],
  //                             key: String): Long = {
  //    val queryClause = transformer.transformQuery(query)
  //    validator.validateQuery(queryClause)
  //    val cnd = buildCondition(queryClause.condition)
  //
  //    // TODO: fix this so it looks like the correct mongo shell command
  //    val description = buildConditionString("distinct", query.collectionName, queryClause)
  //
  //    runCommand(description, queryClause) {
  //      val coll = dbCollectionFactory.getDBCollection(query)
  //      coll.distinct(key, cnd).size()
  //    }
  //  }

  //  def selectCase[F1, CC, S2](f1: M => SelectField[F1, M],
  //                             create: F1 => CC)(implicit ev: AddSelect[State, _, S2]): Query[M, CC, S2] = {
  //    val inst = meta
  //    val fields = List(f1(inst))
  //    val transformer = (xs: List[_]) => create(xs(0).asInstanceOf[F1])
  //    this.copy(select = Some(MongoSelect(fields, transformer)))
  //  }

  def distinct[M <: MB, R](query: Query[M, _, _], key: String, s: RogueSerializer[R])(
      implicit ec: ExecutionContext,
      res: MongoResolution): Future[List[R]] = {
    val queryClause = transformer.transformQuery(query)
    validator.validateQuery(queryClause)
    val cnd = buildCondition(queryClause.condition)

    // TODO: fix this so it looks like the correct mongo shell command
    val description = buildConditionString("distinct", query.collectionName, queryClause)

    runCommand(description, queryClause) {
      queryCollection(query).flatMap { coll =>
        coll.distinct[BSONValue, List](key, selector = Some(cnd)).map { values =>
          val first :: rest = key.split("\\.").toList
          val docFunc: BSONValue => BSONDocument =
            rest.foldLeft[BSONValue => BSONDocument](v => BSONDocument(first -> v)) {
              case (func, keyPart) =>
                func.andThen(d => BSONDocument(keyPart -> d))
            }
          values.map(docFunc andThen s.fromBSONDocument)
        }
      }
    }
  }

  def delete[M <: MB](query: Query[M, _, _], writeConcern: WriteConcern)(
      implicit ec: ExecutionContext,
      res: MongoResolution): Future[WriteResult] = {
    val queryClause = transformer.transformQuery(query)
    validator.validateQuery(queryClause)
    val cnd = buildCondition(queryClause.condition)
    val description = buildConditionString("remove", query.collectionName, queryClause)

    runCommand(description, queryClause) {
      queryCollection(query).flatMap { coll =>
        coll.remove(cnd, writeConcern, firstMatchOnly = false)
      }
    }
  }

  def modify[M <: MB](mod: ModifyQuery[M, _],
                      upsert: Boolean,
                      multi: Boolean,
                      writeConcern: WriteConcern)(implicit ec: ExecutionContext,
                                                  res: MongoResolution): Future[WriteResult] = {
    val modClause = transformer.transformModify(mod)
    validator.validateModify(modClause)
    if (modClause.mod.clauses.nonEmpty) {
      val q = buildCondition(modClause.query.condition)
      val m = buildModify(modClause.mod)
      lazy val description =
        buildModifyString(mod.query.collectionName, modClause, upsert = upsert, multi = multi)

      runCommand(description, modClause.query) {
        queryCollection(modClause.query).flatMap { coll =>
          coll.update(q, m, writeConcern, upsert, multi)
        }
      }
    } else {
      Future.successful(DefaultWriteResult(ok = true, n = 0, Seq(), None, None, None))
    }
  }

  def findAndModify[M <: MB, R](mod: FindAndModifyQuery[M, R],
                                returnNew: Boolean,
                                upsert: Boolean,
                                remove: Boolean)(f: BSONDocument => R)(
      implicit ec: ExecutionContext,
      res: MongoResolution): Future[Option[R]] = {
    val modClause = transformer.transformFindAndModify(mod)
    validator.validateFindAndModify(modClause)
    if (modClause.mod.clauses.nonEmpty || remove) {
      val query = modClause.query
      val cnd = buildCondition(query.condition)
      val ord: Option[BSONDocument] = query.order.map(buildOrder)
      val sel = query.select.map(buildSelect)
      val m = buildModify(modClause.mod)
      lazy val description =
        buildFindAndModifyString(mod.query.collectionName, modClause, returnNew, upsert, remove)

      runCommand(description, modClause.query) {

        val modifyCmd: BSONFindAndModifyCommand.Modify =
          if (remove) BSONFindAndModifyCommand.Remove
          else BSONFindAndModifyCommand.Update(m, returnNew, upsert)
        queryCollection(query).flatMap { coll =>
          coll.findAndModify(cnd, modifyCmd, sort = ord, fields = sel).map(_.result.map(f))
        }
      }
    } else Future.successful(None)
  }

  def queryBuilder[M <: MB](query: Query[M, _, _], batchSize: Option[Int])(
      implicit ec: ExecutionContext,
      res: MongoResolution): Future[GenericQueryBuilder[BSONSerializationPack.type]] = {

    val queryClause = transformer.transformQuery(query)
    validator.validateQuery(queryClause)
    val cnd = buildCondition(queryClause.condition)
    val ord = queryClause.order.map(buildOrder)
    val sel = queryClause.select.map(buildSelect).getOrElse(BSONDocument())

    val opts = QueryOpts(skipN = queryClause.sk.getOrElse(0), batchSizeN = batchSize.getOrElse(0))
    queryCollection(query).map { coll =>
      def _qry = coll.find(cnd, sel).options(opts)
      def qb = ord.fold(_qry)(_qry.sort)
      qb
    }
  }

  def cursor[M <: MB, T: BSONDocumentReader](query: Query[M, _, _], batchSize: Option[Int])(
      implicit ec: ExecutionContext,
      res: MongoResolution): Future[Cursor[T]] = {
    queryBuilder(query, batchSize).map { qb =>
      query.readPreference match {
        case Some(rp) => qb.cursor[T](rp)
        case None => qb.cursor[T]()
      }
    }
  }

  def one[M <: MB, T: BSONDocumentReader](query: Query[M, _, _], batchSize: Option[Int])(
      implicit ec: ExecutionContext,
      res: MongoResolution): Future[Option[T]] = {
    queryBuilder(query, batchSize).flatMap { qb =>
      query.readPreference match {
        case Some(rp) => qb.one[T](rp)
        case None => qb.one[T]
      }
    }
  }
}
