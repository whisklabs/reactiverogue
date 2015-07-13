// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.

package reactiverogue.core

import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.{ DefaultWriteResult, WriteResult, WriteConcern }
import reactiverogue.core.Rogue._
import reactiverogue.core.Iter._
import reactivemongo.bson._
import reactivemongo.api._
import reactivemongo.core.commands._
import scala.concurrent.{ Future, ExecutionContext }
import play.api.libs.iteratee.Iteratee
import reactivemongo.api.collections.GenericQueryBuilder

trait DBCollectionFactory[MB] {
  def getDBCollection[M <: MB](query: Query[M, _, _]): BSONCollection
  def getPrimaryDBCollection[M <: MB](query: Query[M, _, _]): BSONCollection
  def getInstanceName[M <: MB](query: Query[M, _, _]): String
}

class ReactiveMongoAdapter[MB](dbCollectionFactory: DBCollectionFactory[MB]) {

  import QueryHelpers._
  import MongoHelpers.MongoBuilder._

  val EmptyResult =
    LastError(ok = true, err = None, code = None, errMsg = None,
      originalDocument = None, updated = 0, updatedExisting = false)

  //TODO: make it looking for async commands
  private[reactiverogue] def runCommand[M <: MB, T](description: => String,
    query: Query[M, _, _])(f: => T): T = {
    // Use nanoTime instead of currentTimeMillis to time the query since
    // currentTimeMillis only has 10ms granularity on many systems.
    val start = System.nanoTime
    val instanceName: String = dbCollectionFactory.getInstanceName(query)
    try {
      logger.onExecuteQuery(query, instanceName, description, f)
    } catch {
      case e: Exception =>
        throw new RogueException("Mongo query on %s [%s] failed after %d ms".
          format(instanceName, description,
            (System.nanoTime - start) / (1000 * 1000)), e)
    } finally {
      logger.log(query, instanceName, description, (System.nanoTime - start) / (1000 * 1000))
    }
  }

  def count[M <: MB](query: Query[M, _, _])(implicit ec: ExecutionContext): Future[Int] = {
    val queryClause = transformer.transformQuery(query)
    validator.validateQuery(queryClause)
    val condition: BSONDocument = buildCondition(queryClause.condition)
    val description: String = buildConditionString("count", query.collectionName, queryClause)

    runCommand(description, queryClause) {
      val coll = dbCollectionFactory.getDBCollection(query)
      coll.count(selector = Some(condition))
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

  def distinct[M <: MB, R](query: Query[M, _, _], key: String, s: RogueSerializer[R])(implicit ec: ExecutionContext): Future[List[R]] = {
    val queryClause = transformer.transformQuery(query)
    validator.validateQuery(queryClause)
    val cnd = buildCondition(queryClause.condition)

    // TODO: fix this so it looks like the correct mongo shell command
    val description = buildConditionString("distinct", query.collectionName, queryClause)

    runCommand(description, queryClause) {
      val coll = dbCollectionFactory.getDBCollection(query)
      val command = reactivemongo.core.commands.RawCommand(
        BSONDocument(
          "distinct" -> coll.name,
          "key" -> key,
          "query" -> cnd))

      coll.db.command(command).map { bson =>
        val values: List[BSONValue] = bson.getAs[BSONArray]("values").toList.flatMap(_.values)
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

  def delete[M <: MB](query: Query[M, _, _], writeConcern: WriteConcern)(implicit ec: ExecutionContext): Future[WriteResult] = {
    val queryClause = transformer.transformQuery(query)
    validator.validateQuery(queryClause)
    val cnd = buildCondition(queryClause.condition)
    val description = buildConditionString("remove", query.collectionName, queryClause)

    runCommand(description, queryClause) {
      val coll = dbCollectionFactory.getPrimaryDBCollection(query)
      coll.remove(cnd, writeConcern, firstMatchOnly = false)
    }
  }

  def modify[M <: MB](mod: ModifyQuery[M, _],
    upsert: Boolean,
    multi: Boolean,
    writeConcern: WriteConcern)(implicit ec: ExecutionContext): Future[WriteResult] = {
    val modClause = transformer.transformModify(mod)
    validator.validateModify(modClause)
    if (modClause.mod.clauses.nonEmpty) {
      val q = buildCondition(modClause.query.condition)
      val m = buildModify(modClause.mod)
      lazy val description = buildModifyString(mod.query.collectionName, modClause, upsert = upsert, multi = multi)

      runCommand(description, modClause.query) {
        val coll = dbCollectionFactory.getPrimaryDBCollection(modClause.query)
        coll.update(q, m, writeConcern, upsert, multi)
      }
    } else {
      Future.successful(DefaultWriteResult(ok = true, n = 0, Seq(), None, None, None))
    }
  }

  def findAndModify[M <: MB, R](mod: FindAndModifyQuery[M, R],
    returnNew: Boolean,
    upsert: Boolean,
    remove: Boolean)(f: BSONDocument => R)(implicit ec: ExecutionContext): Future[Option[R]] = {
    val modClause = transformer.transformFindAndModify(mod)
    validator.validateFindAndModify(modClause)
    if (modClause.mod.clauses.nonEmpty || remove) {
      val query = modClause.query
      val cnd = buildCondition(query.condition)
      val ord = query.order.map(buildOrder)
      val sel = query.select.map(buildSelect)
      val m = buildModify(modClause.mod)
      lazy val description = buildFindAndModifyString(mod.query.collectionName, modClause, returnNew, upsert, remove)

      runCommand(description, modClause.query) {
        val coll = dbCollectionFactory.getPrimaryDBCollection(query)
        val modifyCmd = if (remove) Remove else Update(m, returnNew)
        val cmd =
          FindAndModify(query.collectionName, query = cnd, modify = modifyCmd,
            upsert = upsert, sort = ord, fields = sel)
        coll.db.command(cmd).map(_.map(f))
      }
    } else Future.successful(None)
  }

  def query[M <: MB](query: Query[M, _, _],
    batchSize: Option[Int])(f: BSONDocument => Unit)(implicit ec: ExecutionContext): Unit = {
    doQuery("find", query, batchSize) { cursor =>
      cursor.enumerate().apply(Iteratee.foreach(f))
    }
  }

  def queryBuilder[M <: MB](query: Query[M, _, _], batchSize: Option[Int])(implicit ec: ExecutionContext): GenericQueryBuilder[BSONSerializationPack.type] = {

    val queryClause = transformer.transformQuery(query)
    validator.validateQuery(queryClause)
    val cnd = buildCondition(queryClause.condition)
    val ord = queryClause.order.map(buildOrder)
    val sel = queryClause.select.map(buildSelect).getOrElse(BSONDocument())
    //    val hnt = queryClause.hint.map(buildHint)

    val coll = dbCollectionFactory.getDBCollection(query)
    val opts = QueryOpts(skipN = queryClause.sk.getOrElse(0), batchSizeN = batchSize.getOrElse(0))
    def _qry = coll.find(cnd, sel).options(opts)
    def qb = ord.fold(_qry)(_qry.sort)
    qb
  }

  def cursor[M <: MB, T: BSONDocumentReader](query: Query[M, _, _], batchSize: Option[Int])(implicit ec: ExecutionContext): Cursor[T] = {
    val qb = queryBuilder(query, batchSize)
    query.readPreference match {
      case Some(rp) => qb.cursor[T](rp)
      case None => qb.cursor[T]()
    }
  }

  def one[M <: MB, T: BSONDocumentReader](query: Query[M, _, _], batchSize: Option[Int])(implicit ec: ExecutionContext): Future[Option[T]] = {
    val qb = queryBuilder(query, batchSize)
    query.readPreference match {
      case Some(rp) => qb.one[T](rp)
      case None => qb.one[T]
    }
  }

  def doQuery[M <: MB, T](
    operation: String,
    query: Query[M, _, _],
    batchSize: Option[Int])(f: Cursor[BSONDocument] => T)(implicit ec: ExecutionContext): T = {
    val queryClause = transformer.transformQuery(query)

    lazy val description = buildQueryString(operation, query.collectionName, queryClause)

    runCommand(description, queryClause) {
      val coll = dbCollectionFactory.getDBCollection(query)
      try {
        //        val cursor = cursor
        f(cursor[M, BSONDocument](query, batchSize))
      } catch {
        case e: Exception =>
          throw new RogueException("Mongo query on %s [%s] failed".format(
            coll.db.name, description), e)
      }
    }
  }
}
