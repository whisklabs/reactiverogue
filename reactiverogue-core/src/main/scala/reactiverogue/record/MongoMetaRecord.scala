package reactiverogue.record

import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.{WriteConcern, WriteResult}
import reactivemongo.bson._
import reactiverogue.db.MongoResolution

import scala.concurrent.{ExecutionContext, Future}

trait MongoMetaRecord[BaseRecord <: MongoRecord[BaseRecord]] extends BsonMetaRecord[BaseRecord] {

  self: BaseRecord =>

  def collectionName: String

  /**
    * Delete the instance from backing store
    */
  def delete_!(inst: BaseRecord)(implicit ec: ExecutionContext,
                                 res: MongoResolution): Future[WriteResult] = {
    res.database.flatMap(db =>
    db.apply[BSONCollection](collectionName)
      .remove(BSONDocument("_id" -> inst.id.value), WriteConcern.Default, firstMatchOnly = true))
  }

  def bulkDelete_!!(qry: BSONDocument)(implicit ec: ExecutionContext,
                                       res: MongoResolution): Future[WriteResult] = {
    res.database.flatMap(db =>
    db.apply[BSONCollection](collectionName)
      .remove(qry, WriteConcern.Default, firstMatchOnly = false))
  }

  def bulkDelete_!!!(implicit ec: ExecutionContext, res: MongoResolution): Future[WriteResult] = {
    res.database.flatMap(db =>
    db.apply[BSONCollection](collectionName)
      .remove(BSONDocument.empty, WriteConcern.Default, firstMatchOnly = false))
  }

  def count(selector: Option[BSONDocument] = None)(implicit ec: ExecutionContext,
                                                   res: MongoResolution): Future[Int] = {
    res.database.flatMap(db =>
    db.apply[BSONCollection](collectionName).count(selector))
  }

  /**
    * Save the instance in the appropriate backing store
    */
  def save(inst: BaseRecord, concern: WriteConcern)(implicit ec: ExecutionContext,
                                                    res: MongoResolution): Future[WriteResult] = {
    res.database.flatMap {db =>
    val coll = db.apply[BSONCollection](collectionName)
    inst.id.valueOpt match {
      case None => coll.insert(inst.id(BSONObjectID.generate).asBSONDocument, concern)
      case Some(id) =>
        coll.update(BSONDocument(Seq("_id" -> id)), update = inst.asBSONDocument, upsert = true)
    }
    }
  }

}
