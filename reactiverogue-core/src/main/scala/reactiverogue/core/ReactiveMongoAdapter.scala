// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.

package reactiverogue.core

import reactiverogue.core.Rogue._
import reactiverogue.core.Iter._
import reactivemongo.bson._
import reactivemongo.api._
import reactivemongo.api.collections.default._
import reactivemongo.core.commands._
import scala.collection.mutable.ListBuffer
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
      val db = coll.db

      val cmd = Count(query.collectionName, query = Some(condition))

      db.command(cmd)
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

  //  def distinct[M <: MB, R](query: Query[M, _, _],
  //                           key: String): List[R] = {
  //    val queryClause = transformer.transformQuery(query)
  //    validator.validateQuery(queryClause)
  //    val cnd = buildCondition(queryClause.condition)
  //
  //    // TODO: fix this so it looks like the correct mongo shell command
  //    val description = buildConditionString("distinct", query.collectionName, queryClause)
  //
  //    runCommand(description, queryClause) {
  //      val coll = dbCollectionFactory.getDBCollection(query)
  //      val rv = new ListBuffer[R]
  //      val rj = coll.distinct(key, cnd)
  //      for (i <- 0 until rj.size) rv += rj.get(i).asInstanceOf[R]
  //      rv.toList
  //    }
  //  }

  def delete[M <: MB](query: Query[M, _, _], writeConcern: GetLastError)(implicit ec: ExecutionContext): Future[LastError] = {
    val queryClause = transformer.transformQuery(query)
    validator.validateQuery(queryClause)
    val cnd = buildCondition(queryClause.condition)
    val description = buildConditionString("remove", query.collectionName, queryClause)

    runCommand(description, queryClause) {
      val coll = dbCollectionFactory.getPrimaryDBCollection(query)
      coll.remove(cnd, writeConcern, false)
    }
  }

  def modify[M <: MB](mod: ModifyQuery[M, _],
    upsert: Boolean,
    multi: Boolean,
    writeConcern: GetLastError)(implicit ec: ExecutionContext): Future[LastError] = {
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
      Future.successful(EmptyResult)
    }
  }

  def findAndModify[M <: MB, R](mod: FindAndModifyQuery[M, R],
    returnNew: Boolean,
    upsert: Boolean,
    remove: Boolean)(f: BSONDocument => R)(implicit ec: ExecutionContext): Future[Option[R]] = {
    val modClause = transformer.transformFindAndModify(mod)
    validator.validateFindAndModify(modClause)
    if (!modClause.mod.clauses.isEmpty || remove) {
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

  def queryBuilder[M <: MB](query: Query[M, _, _], batchSize: Option[Int])(implicit ec: ExecutionContext): GenericQueryBuilder[BSONDocument, BSONDocumentReader, BSONDocumentWriter] = {

    val queryClause = transformer.transformQuery(query)
    validator.validateQuery(queryClause)
    val cnd = buildCondition(queryClause.condition)
    val ord = queryClause.order.map(buildOrder)
    val sel = queryClause.select.map(buildSelect).getOrElse(BSONDocument())
    val hnt = queryClause.hint.map(buildHint)

    val coll = dbCollectionFactory.getDBCollection(query)
    val opts = QueryOpts(skipN = queryClause.sk.getOrElse(0), batchSizeN = batchSize.getOrElse(0))
    def _qry = coll.find(cnd, sel).options(opts)
    def qb = ord.map(_qry.sort).getOrElse(_qry)
    qb
  }

  //  def iterate[M <: MB, R, S](query: Query[M, R, _],
  //                             initialState: S,
  //                             f: DBObject => R,
  //                             readPreference: Option[ReadPreference] = None)
  //                            (handler: (S, Event[R]) => Command[S]): S = {
  //    def getObject(cursor: DBCursor): Either[Exception, R] = {
  //      try {
  //        Right(f(cursor.next))
  //      } catch {
  //        case e: Exception => Left(e)
  //      }
  //    }
  //
  //    @scala.annotation.tailrec
  //    def iter(cursor: DBCursor, curState: S): S = {
  //      if (cursor.hasNext) {
  //        getObject(cursor) match {
  //          case Left(e) => handler(curState, Error(e)).state
  //          case Right(r) => handler(curState, Item(r)) match {
  //            case Continue(s) => iter(cursor, s)
  //            case Return(s) => s
  //          }
  //        }
  //      } else {
  //        handler(curState, EOF).state
  //      }
  //    }
  //
  //    doQuery("find", query, None, readPreference)(cursor =>
  //      iter(cursor, initialState)
  //    )
  //  }
  //
  //  def iterateBatch[M <: MB, R, S](query: Query[M, R, _],
  //                                  batchSize: Int,
  //                                  initialState: S,
  //                                  f: BSONDocument => R)
  //                                 (handler: (S, Event[List[R]]) => Command[S]): S = {
  //    val buf = new ListBuffer[R]
  //
  //    def getBatch(cursor: DBCursor): Either[Exception, List[R]] = {
  //      try {
  //        buf.clear()
  //        // ListBuffer#length is O(1) vs ListBuffer#size is O(N) (true in 2.9.x, fixed in 2.10.x)
  //        while (cursor.hasNext && buf.length < batchSize) {
  //          buf += f(cursor.next)
  //        }
  //        Right(buf.toList)
  //      } catch {
  //        case e: Exception => Left(e)
  //      }
  //    }
  //
  //    @scala.annotation.tailrec
  //    def iter(cursor: DBCursor, curState: S): S = {
  //      if (cursor.hasNext) {
  //        getBatch(cursor) match {
  //          case Left(e) => handler(curState, Error(e)).state
  //          case Right(Nil) => handler(curState, EOF).state
  //          case Right(rs) => handler(curState, Item(rs)) match {
  //            case Continue(s) => iter(cursor, s)
  //            case Return(s) => s
  //          }
  //        }
  //      } else {
  //        handler(curState, EOF).state
  //      }
  //    }
  //
  //    doQuery("find", query, Some(batchSize))(cursor => {
  //      iter(cursor, initialState)
  //    })
  //  }

  //  def explain[M <: MB](query: Query[M, _, _]): String = {
  //    doQuery("find", query, None){cursor =>
  //      cursor.explain.toString
  //    }
  //  }

  def doQuery[M <: MB, T](
    operation: String,
    query: Query[M, _, _],
    batchSize: Option[Int])(f: Cursor[BSONDocument] => T)(implicit ec: ExecutionContext): T = {
    val queryClause = transformer.transformQuery(query)

    lazy val description = buildQueryString(operation, query.collectionName, queryClause)

    runCommand(description, queryClause) {
      val coll = dbCollectionFactory.getDBCollection(query)
      val qb = queryBuilder(query, batchSize)
      try {
        val cursor = qb.cursor[BSONDocument]
        // Always apply batchSize *before* limit. If the caller passes a negative value to limit(),
        // the driver applies it instead to batchSize. (A negative batchSize means, return one batch
        // and close the cursor.) Then if we set batchSize, the negative "limit" is overwritten, and
        // the query executes without a limit.
        // http://api.mongodb.org/java/2.7.3/com/mongodb/DBCursor.html#limit(int)
        //        batchSize.foreach(cursor batchSize _)
        //        queryClause.lim.foreach(cursor.limit _)
        //        queryClause.sk.foreach(cursor.skip _)
        //        ord.foreach(cursor.sort _)
        //        queryClause.maxScan.foreach(cursor addSpecial("$maxScan", _))
        //        queryClause.comment.foreach(cursor addSpecial("$comment", _))
        //        hnt.foreach(cursor hint _)
        f(cursor)
      } catch {
        case e: Exception =>
          throw new RogueException("Mongo query on %s [%s] failed".format(
            coll.db.name, description), e)
      }
    }
  }
}
