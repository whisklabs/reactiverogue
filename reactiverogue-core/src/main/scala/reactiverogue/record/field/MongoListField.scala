package reactiverogue.record
package field

import reactiverogue.bson.BSONSerializable

class MongoListField[OwnerType <: BsonRecord[OwnerType], ListType: BSONSerializable](
    rec: OwnerType)
    extends DirectBsonField[List[ListType]]
    with RequiredRecordField[List[ListType], OwnerType] {

  def owner = rec

  override def defaultValue: List[ListType] = Nil

}
