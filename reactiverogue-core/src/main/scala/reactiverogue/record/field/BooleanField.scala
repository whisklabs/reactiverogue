package reactiverogue.record
package field

class BooleanField[OwnerType <: BsonRecord[OwnerType]](rec: OwnerType)
    extends DirectBsonField[Boolean]
    with RequiredRecordField[Boolean, OwnerType] {

  override def defaultValue = false

  def this(rec: OwnerType, value: Boolean) = {
    this(rec)
    set(value)
  }

  def owner = rec
}

class OptionalBooleanField[OwnerType <: BsonRecord[OwnerType]](rec: OwnerType)
    extends DirectBsonField[Boolean]
    with OptionalRecordField[Boolean, OwnerType] {

  def this(rec: OwnerType, value: Option[Boolean]) = {
    this(rec)
    setOption(value)
  }

  def owner = rec
}
