package reactiverogue.record
package field

import reactivemongo.bson.BSONObjectID

trait ObjectIdPk[OwnerType <: MongoRecord[OwnerType]] { self: OwnerType =>

  def defaultIdValue = BSONObjectID.generate

  object id extends ObjectIdField(this.asInstanceOf[OwnerType]) {
    override def name = "_id"
    override def defaultValue = defaultIdValue
  }
}
