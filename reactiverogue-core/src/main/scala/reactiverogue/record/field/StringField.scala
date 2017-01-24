package reactiverogue.record
package field

class StringField[OwnerType <: BsonRecord[OwnerType]](rec: OwnerType)
    extends DirectBsonField[String]
    with RequiredRecordField[String, OwnerType] {

  override def defaultValue = null.asInstanceOf[String]

  def this(rec: OwnerType, value: String) = {
    this(rec)
    set(value)
  }

  def owner = rec
}

class OptionalStringField[OwnerType <: BsonRecord[OwnerType]](rec: OwnerType)
    extends DirectBsonField[String]
    with OptionalRecordField[String, OwnerType] {

  def this(rec: OwnerType, value: Option[String]) = {
    this(rec)
    setOption(value)
  }

  def owner = rec
}
