package reactiverogue.record

import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.{WriteConcern, WriteResult}
import reactivemongo.bson._

import scala.concurrent.{ExecutionContext, Future}

trait MongoMetaRecord[BaseRecord <: MongoRecord[BaseRecord]]
    extends BsonMetaRecord[BaseRecord] {

  self: BaseRecord =>

  def collectionName: String

  /**
   * Delete the instance from backing store
   */
  def delete_!(inst: BaseRecord)(implicit ec: ExecutionContext, db: DefaultDB): Future[WriteResult] = {
    db.apply[BSONCollection](collectionName).remove(BSONDocument("_id" -> inst.id.value), WriteConcern.Default, firstMatchOnly = true)
  }

  def bulkDelete_!!(qry: BSONDocument)(implicit ec: ExecutionContext, db: DefaultDB): Future[WriteResult] = {
    db.apply[BSONCollection](collectionName).remove(qry, WriteConcern.Default, firstMatchOnly = false)
  }

  /**
   * Save the instance in the appropriate backing store
   */
  def save(inst: BaseRecord, concern: WriteConcern)(implicit ec: ExecutionContext, db: DefaultDB): Future[WriteResult] = {
    val coll = db.apply[BSONCollection](collectionName)
    inst.id.valueOpt match {
      case None => coll.insert(inst.asBSONDocument, concern)
      case Some(id) => coll.update(BSONDocument(Seq("_id" -> id)), update = inst.asBSONDocument, upsert = true)
    }
  }

}
