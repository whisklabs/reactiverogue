package reactiverogue.record
package field

class LongField[OwnerType <: BsonRecord[OwnerType]](rec: OwnerType)
    extends DirectBsonField[Long] with RequiredRecordField[Long, OwnerType] {

  override def defaultValue: Long = null.asInstanceOf[Long]

  def this(rec: OwnerType, value: Long) = {
    this(rec)
    set(value)
  }

  def owner = rec
}

class OptionalLongField[OwnerType <: BsonRecord[OwnerType]](rec: OwnerType)
    extends DirectBsonField[Long] with OptionalRecordField[Long, OwnerType] {

  def this(rec: OwnerType, value: Option[Long]) = {
    this(rec)
    setOption(value)
  }

  def owner = rec
}

