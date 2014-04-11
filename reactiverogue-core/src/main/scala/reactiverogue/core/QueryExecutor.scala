// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.

package reactiverogue.core

import com.foursquare.field.Field
import reactiverogue.core.MongoHelpers.{ MongoModify, MongoSelect }
import scala.collection.mutable.ListBuffer
import reactivemongo.core.commands.GetLastError
import reactivemongo.bson.BSONDocument
import scala.concurrent.{ Future, ExecutionContext }
import reactivemongo.bson.BSONDocumentReader
import reactivemongo.core.commands.LastError

trait RogueSerializer[R] extends BSONDocumentReader[R] {
  def fromBSONDocument(doc: BSONDocument): R

  override def read(doc: BSONDocument): R = fromBSONDocument(doc)
}

trait QueryExecutor[MB] extends Rogue {
  def adapter: ReactiveMongoAdapter[MB]
  def optimizer: QueryOptimizer

  def defaultWriteConcern: GetLastError

  val EmptyResult =
    LastError(ok = true, err = None, code = None, errMsg = None,
      originalDocument = None, updated = 0, updatedExisting = false)

  protected def serializer[M <: MB, R](
    meta: M,
    select: Option[MongoSelect[M, R]]): RogueSerializer[R]

  def count[M <: MB, State](query: Query[M, _, State])(implicit ev: ShardingOk[M, State], ec: ExecutionContext): Future[Int] = {
    if (optimizer.isEmptyQuery(query)) {
      Future.successful(0)
    } else {
      adapter.count(query)
    }
  }

  //  def countDistinct[M <: MB, V, State](query: Query[M, _, State])
  //                                      (field: M => Field[V, M])
  //                                      (implicit ev: ShardingOk[M, State]): Long = {
  //    if (optimizer.isEmptyQuery(query)) {
  //      0L
  //    } else {
  //      adapter.countDistinct(query, field(query.meta).name)
  //    }
  //  }
  //
  //  def distinct[M <: MB, V, State](query: Query[M, _, State])
  //                                 (field: M => Field[V, M])
  //                                 (implicit ev: ShardingOk[M, State]): List[V] = {
  //    if (optimizer.isEmptyQuery(query)) {
  //      Nil
  //    } else {
  //      adapter.distinct(query, field(query.meta).name)
  //    }
  //  }

  def fetch[M <: MB, R, State](query: Query[M, R, State])(implicit ev: ShardingOk[M, State], ec: ExecutionContext): Future[List[R]] = {
    if (optimizer.isEmptyQuery(query)) {
      Future.successful(Nil)
    } else {
      implicit val s = serializer[M, R](query.meta, query.select)
      val cursor = adapter.queryBuilder(query, None).cursor[R]
      query.lim match {
        case Some(limit) => cursor.collect[List](limit)
        case None => cursor.collect[List]()
      }
    }
  }

  def fetchOne[M <: MB, R, State, S2](query: Query[M, R, State])(implicit ev1: AddLimit[State, S2], ev2: ShardingOk[M, S2], ec: ExecutionContext): Future[Option[R]] = {
    if (optimizer.isEmptyQuery(query)) {
      Future.successful(None)
    } else {
      implicit val s = serializer[M, R](query.meta, query.select)
      adapter.queryBuilder(query, None).one[R]
    }
  }

  def foreach[M <: MB, R, State](query: Query[M, R, State])(f: R => Unit)(implicit ev: ShardingOk[M, State], ec: ExecutionContext): Unit = {
    if (optimizer.isEmptyQuery(query)) {
      ()
    } else {
      val s = serializer[M, R](query.meta, query.select)
      adapter.query(query, None)(dbo => f(s.fromBSONDocument(dbo)))
    }
  }

  private def drainBuffer[A, B](
    from: ListBuffer[A],
    to: ListBuffer[B],
    f: List[A] => List[B],
    size: Int): Unit = {
    // ListBuffer#length is O(1) vs ListBuffer#size is O(N) (true in 2.9.x, fixed in 2.10.x)
    if (from.length >= size) {
      to ++= f(from.toList)
      from.clear
    }
  }

  //  def fetchBatch[M <: MB, R, T, State](query: Query[M, R, State],
  //                                       batchSize: Int)
  //                                      (f: List[R] => List[T])
  //                                      (implicit ev: ShardingOk[M, State]): List[T] = {
  //    if (optimizer.isEmptyQuery(query)) {
  //      Nil
  //    } else {
  //      val s = serializer[M, R](query.meta, query.select)
  //      val rv = new ListBuffer[T]
  //      val buf = new ListBuffer[R]
  //
  //      adapter.query(query, Some(batchSize)) { dbo =>
  //        buf += s.fromBSONDocument(dbo)
  //        drainBuffer(buf, rv, f, batchSize)
  //      }
  //      drainBuffer(buf, rv, f, 1)
  //
  //      rv.toList
  //    }
  //  }

  def bulkDelete_!![M <: MB, State](query: Query[M, _, State],
    writeConcern: GetLastError = defaultWriteConcern)(implicit ev1: Required[State, Unselected with Unlimited with Unskipped],
      ev2: ShardingOk[M, State], ec: ExecutionContext): Future[LastError] = {
    if (optimizer.isEmptyQuery(query)) {
      Future.failed(new Exception("empty"))
    } else {
      adapter.delete(query, writeConcern)
    }
  }

  def updateOne[M <: MB, State](
    query: ModifyQuery[M, State],
    writeConcern: GetLastError = defaultWriteConcern)(implicit ev: RequireShardKey[M, State], ec: ExecutionContext): Future[LastError] = {
    if (optimizer.isEmptyQuery(query)) {
      Future.successful(EmptyResult)
    } else {
      adapter.modify(query, upsert = false, multi = false, writeConcern = writeConcern)
    }
  }

  def upsertOne[M <: MB, State](
    query: ModifyQuery[M, State],
    writeConcern: GetLastError = defaultWriteConcern)(implicit ev: RequireShardKey[M, State], ec: ExecutionContext): Future[LastError] = {
    if (optimizer.isEmptyQuery(query)) {
      Future.successful(EmptyResult)
    } else {
      adapter.modify(query, upsert = true, multi = false, writeConcern = writeConcern)
    }
  }

  def updateMulti[M <: MB, State](
    query: ModifyQuery[M, State],
    writeConcern: GetLastError = defaultWriteConcern)(implicit ec: ExecutionContext): Future[LastError] = {
    if (optimizer.isEmptyQuery(query)) {
      Future.successful(EmptyResult)
    } else {
      adapter.modify(query, upsert = false, multi = true, writeConcern = writeConcern)
    }
  }

  def findAndUpdateOne[M <: MB, R](
    query: FindAndModifyQuery[M, R],
    returnNew: Boolean = false,
    writeConcern: GetLastError = defaultWriteConcern)(implicit ec: ExecutionContext): Future[Option[R]] = {
    if (optimizer.isEmptyQuery(query)) {
      Future.successful(None)
    } else {
      val s = serializer[M, R](query.query.meta, query.query.select)
      adapter.findAndModify(query, returnNew, upsert = false, remove = false)(s.fromBSONDocument)
    }
  }

  def findAndUpsertOne[M <: MB, R](
    query: FindAndModifyQuery[M, R],
    returnNew: Boolean = false,
    writeConcern: GetLastError = defaultWriteConcern)(implicit ec: ExecutionContext): Future[Option[R]] = {
    if (optimizer.isEmptyQuery(query)) {
      Future.successful(None)
    } else {
      val s = serializer[M, R](query.query.meta, query.query.select)
      adapter.findAndModify(query, returnNew, upsert = true, remove = false)(s.fromBSONDocument)
    }
  }

  def findAndDeleteOne[M <: MB, R, State](
    query: Query[M, R, State],
    writeConcern: GetLastError = defaultWriteConcern)(implicit ev: RequireShardKey[M, State], ec: ExecutionContext): Future[Option[R]] = {
    if (optimizer.isEmptyQuery(query)) {
      Future.successful(None)
    } else {
      val s = serializer[M, R](query.meta, query.select)
      val mod = FindAndModifyQuery(query, MongoModify(Nil))
      adapter.findAndModify(mod, returnNew = false, upsert = false, remove = true)(s.fromBSONDocument)
    }
  }

  //  def explain[M <: MB](query: Query[M, _, _]): String = {
  //    adapter.explain(query)
  //  }

  //  def iterate[S, M <: MB, R, State](query: Query[M, R, State],
  //                                    state: S)
  //                                   (handler: (S, Iter.Event[R]) => Iter.Command[S])
  //                                   (implicit ev: ShardingOk[M, State]): S = {
  //    if (optimizer.isEmptyQuery(query)) {
  //      handler(state, Iter.EOF).state
  //    } else {
  //      val s = serializer[M, R](query.meta, query.select)
  //      adapter.iterate(query, state, s.fromBSONDocument _)(handler)
  //    }
  //  }

  //  def iterateBatch[S, M <: MB, R, State](query: Query[M, R, State],
  //                                         batchSize: Int,
  //                                         state: S)
  //                                        (handler: (S, Iter.Event[List[R]]) => Iter.Command[S])
  //                                        (implicit ev: ShardingOk[M, State]): S = {
  //    if (optimizer.isEmptyQuery(query)) {
  //      handler(state, Iter.EOF).state
  //    } else {
  //      val s = serializer[M, R](query.meta, query.select)
  //      adapter.iterateBatch(query, batchSize, state, s.fromBSONDocument _)(handler)
  //    }
  //  }
}
