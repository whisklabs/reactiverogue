package reactiverogue.record
package field

import reactivemongo.bson._

/*
 * List of BsonRecords
 */
abstract class BsonRecordListField[OwnerType <: BsonRecord[OwnerType], SubRecordType <: BsonRecord[SubRecordType]](rec: OwnerType, valueMeta: BsonMetaRecord[SubRecordType])(implicit mf: Manifest[SubRecordType])
    extends RequiredRecordField[List[SubRecordType], OwnerType] {

  def owner: OwnerType = rec

  override def defaultValue: List[SubRecordType] = Nil
  override def isOptional = true

  def zero: SubRecordType

  def asBSONValue: BSONValue =
    BSONArray(value.map(_.asBSONDocument))

  def setFromBSONValue(value: BSONValue): Option[List[SubRecordType]] = value match {
    case x: BSONArray => setOption(Some(x.values.collect({ case v: BSONDocument => valueMeta.fromBSONDocument(v) }).toList))
    case _ => setOption(None)
  }

}
