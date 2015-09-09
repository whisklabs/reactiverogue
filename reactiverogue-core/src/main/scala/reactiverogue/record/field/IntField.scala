package reactiverogue.record
package field

class IntField[OwnerType <: BsonRecord[OwnerType]](rec: OwnerType)
    extends DirectBsonField[Int] with RequiredRecordField[Int, OwnerType] {

  override def defaultValue: Int = null.asInstanceOf[Int]

  def this(rec: OwnerType, value: Int) = {
    this(rec)
    set(value)
  }

  def owner = rec
}

class OptionalIntField[OwnerType <: BsonRecord[OwnerType]](rec: OwnerType)
    extends DirectBsonField[Int] with OptionalRecordField[Int, OwnerType]{

  def this(rec: OwnerType, value: Option[Int]) = {
    this(rec)
    setOption(value)
  }

  def owner = rec
}

