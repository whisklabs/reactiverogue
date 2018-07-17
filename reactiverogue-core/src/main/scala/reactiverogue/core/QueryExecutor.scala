package reactiverogue.core

import play.api.libs.iteratee.{Enumerator, Iteratee}
import reactivemongo.api.commands.{DefaultWriteResult, LastError, WriteConcern, WriteResult}
import reactivemongo.bson.{BSONDocument, BSONDocumentReader}
import reactivemongo.play.iteratees._
import reactiverogue.core.MongoHelpers.{MongoModify, MongoSelect}
import reactiverogue.db.MongoResolution

import scala.concurrent.{ExecutionContext, Future}

trait RogueSerializer[R] extends BSONDocumentReader[R] {
  def fromBSONDocument(doc: BSONDocument): R

  override def read(doc: BSONDocument): R = fromBSONDocument(doc)
}

trait QueryExecutor[MB] extends Rogue {
  def adapter: ReactiveMongoAdapter[MB]
  def optimizer: QueryOptimizer

  def defaultWriteConcern: WriteConcern

  val EmptyResult =
    LastError(ok = true,
              errmsg = None,
              code = None,
              lastOp = None,
              n = 0,
              singleShard = None,
              updatedExisting = false,
              upserted = None,
              wnote = None,
              wtimeout = false,
              waited =None,
              wtime = None)

  val EmptyWriteResult =
    DefaultWriteResult(ok = true,
                       n = 0,
                       writeErrors = Seq(),
                       writeConcernError = None,
                       code = None,
                       errmsg = None)

  protected def serializer[M <: MB, R](meta: M,
                                       select: Option[MongoSelect[M, R]]): RogueSerializer[R]

  def count[M <: MB, State](query: Query[M, _, State])(implicit ev: ShardingOk[M, State],
                                                       ec: ExecutionContext,
                                                       res: MongoResolution): Future[Int] = {
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
  def distinct[M <: MB, V, State](query: Query[M, _, State])(f1: M => SelectField[V, _])(
      implicit ev: ShardingOk[M, State],
      ec: ExecutionContext,
      res: MongoResolution): Future[List[V]] = {
    if (optimizer.isEmptyQuery(query)) {
      Future.successful(Nil)
    } else {
      val inst = query.meta
      val fields = List(f1(inst).asInstanceOf[SelectField[V, M]])
      val key = f1(query.meta).field.name
      val _transformer = (xs: List[_]) => xs.head.asInstanceOf[V]
      val s = serializer[M, V](inst, Some(MongoSelect(fields, _transformer)))
      adapter.distinct(query, key, s)
    }
  }

  def fetch[M <: MB, R, State](query: Query[M, R, State])(
      implicit ev: ShardingOk[M, State],
      ec: ExecutionContext,
      res: MongoResolution): Future[List[R]] = {
    if (optimizer.isEmptyQuery(query)) {
      Future.successful(Nil)
    } else {
      implicit val s = serializer[M, R](query.meta, query.select)
      adapter.cursor(query, None).flatMap { cursor =>
        query.lim match {
          case Some(limit) => cursor.collect[List](limit)
          case None => cursor.collect[List]()
        }
      }
    }
  }

  def fetchEnumerator[M <: MB, R, State](query: Query[M, R, State])(
      implicit ev: ShardingOk[M, State],
      ec: ExecutionContext,
      res: MongoResolution): Enumerator[R] = {
    if (optimizer.isEmptyQuery(query)) {
      Enumerator.empty
    } else {
      implicit val s = serializer[M, R](query.meta, query.select)

      Enumerator.flatten {
        adapter.cursor(query, None).map { _cursor =>
          val cursor = cursorProducer.produce(_cursor)
          query.lim match {
            case Some(limit) => cursor.enumerator(maxDocs = limit)
            case None => cursor.enumerator()
          }
        }
      }
    }
  }

  def fetchOne[M <: MB, R, State, S2](query: Query[M, R, State])(
      implicit ev1: AddLimit[State, S2],
      ev2: ShardingOk[M, S2],
      ec: ExecutionContext,
      res: MongoResolution): Future[Option[R]] = {
    if (optimizer.isEmptyQuery(query)) {
      Future.successful(None)
    } else {
      implicit val s = serializer[M, R](query.meta, query.select)
      adapter.one[M, R](query, None)
    }
  }

  def foreach[M <: MB, R, State](query: Query[M, R, State])(f: R => Unit)(
      implicit ev: ShardingOk[M, State],
      ec: ExecutionContext,
      res: MongoResolution): Future[Unit] = {
    if (optimizer.isEmptyQuery(query)) {
      Future.successful(())
    } else {
      fetchEnumerator[M, R, State](query).run(Iteratee.foreach(f))
    }
  }

  def bulkDelete_!![M <: MB, State](query: Query[M, _, State],
                                    writeConcern: WriteConcern = defaultWriteConcern)(
      implicit ev1: Required[State, Unselected with Unlimited with Unskipped],
      ev2: ShardingOk[M, State],
      ec: ExecutionContext,
      res: MongoResolution): Future[WriteResult] = {
    if (optimizer.isEmptyQuery(query)) {
      Future.failed(new Exception("empty"))
    } else {
      adapter.delete(query, writeConcern)
    }
  }

  def updateOne[M <: MB, State](query: ModifyQuery[M, State],
                                writeConcern: WriteConcern = defaultWriteConcern)(
      implicit ev: RequireShardKey[M, State],
      ec: ExecutionContext,
      res: MongoResolution): Future[WriteResult] = {
    if (optimizer.isEmptyQuery(query)) {
      Future.successful(EmptyWriteResult)
    } else {
      adapter.modify(query, upsert = false, multi = false, writeConcern = writeConcern)
    }
  }

  def upsertOne[M <: MB, State](query: ModifyQuery[M, State],
                                writeConcern: WriteConcern = defaultWriteConcern)(
      implicit ev: RequireShardKey[M, State],
      ec: ExecutionContext,
      res: MongoResolution): Future[WriteResult] = {
    if (optimizer.isEmptyQuery(query)) {
      Future.successful(EmptyWriteResult)
    } else {
      adapter.modify(query, upsert = true, multi = false, writeConcern = writeConcern)
    }
  }

  def updateMulti[M <: MB, State](query: ModifyQuery[M, State],
                                  writeConcern: WriteConcern = defaultWriteConcern)(
      implicit ec: ExecutionContext,
      res: MongoResolution): Future[WriteResult] = {
    if (optimizer.isEmptyQuery(query)) {
      Future.successful(EmptyWriteResult)
    } else {
      adapter.modify(query, upsert = false, multi = true, writeConcern = writeConcern)
    }
  }

  def findAndUpdateOne[M <: MB, R](query: FindAndModifyQuery[M, R], returnNew: Boolean = false)(
      implicit ec: ExecutionContext,
      res: MongoResolution): Future[Option[R]] = {
    if (optimizer.isEmptyQuery(query)) {
      Future.successful(None)
    } else {
      val s = serializer[M, R](query.query.meta, query.query.select)
      adapter.findAndModify(query, returnNew, upsert = false, remove = false)(s.fromBSONDocument)
    }
  }

  def findAndUpsertOne[M <: MB, R](query: FindAndModifyQuery[M, R], returnNew: Boolean = false)(
      implicit ec: ExecutionContext,
      res: MongoResolution): Future[Option[R]] = {
    if (optimizer.isEmptyQuery(query)) {
      Future.successful(None)
    } else {
      val s = serializer[M, R](query.query.meta, query.query.select)
      adapter.findAndModify(query, returnNew, upsert = true, remove = false)(s.fromBSONDocument)
    }
  }

  def findAndDeleteOne[M <: MB, R, State](query: Query[M, R, State])(
      implicit ev: RequireShardKey[M, State],
      ec: ExecutionContext,
      res: MongoResolution): Future[Option[R]] = {
    if (optimizer.isEmptyQuery(query)) {
      Future.successful(None)
    } else {
      val s = serializer[M, R](query.meta, query.select)
      val mod = FindAndModifyQuery(query, MongoModify(Nil))
      adapter.findAndModify(mod, returnNew = false, upsert = false, remove = true)(
        s.fromBSONDocument)
    }
  }
}
