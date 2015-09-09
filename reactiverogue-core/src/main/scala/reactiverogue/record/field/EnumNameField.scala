package reactiverogue.record
package field

import reactivemongo.bson._

trait EnumNameTypedField[EnumType <: Enumeration] extends BsonField[EnumType#Value] {
  protected val enum: EnumType
  protected val valueManifest: Manifest[EnumType#Value]

  def asBSONValue: BSONValue =
    valueOpt.map(v => BSONString(v.toString)).getOrElse(BSONUndefined)

  def setFromBSONValue(value: BSONValue): Option[EnumType#Value] = value match {
    case BSONString(s) => setOption(enum.values.find(_.toString == s))
    case _ => setOption(None)
  }
}

class EnumNameField[OwnerType <: BsonRecord[OwnerType], EnumType <: Enumeration](rec: OwnerType, protected val enum: EnumType)(implicit m: Manifest[EnumType#Value])
    extends RequiredRecordField[EnumType#Value, OwnerType] with EnumNameTypedField[EnumType] {

  def this(rec: OwnerType, enum: EnumType, value: EnumType#Value)(implicit m: Manifest[EnumType#Value]) = {
    this(rec, enum)
    set(value)
  }

  def defaultValue: EnumType#Value = enum.values.iterator.next()

  def owner = rec
  protected val valueManifest = m
}

class OptionalEnumNameField[OwnerType <: BsonRecord[OwnerType], EnumType <: Enumeration](rec: OwnerType, protected val enum: EnumType)(implicit m: Manifest[EnumType#Value])
    extends OptionalRecordField[EnumType#Value, OwnerType] with EnumNameTypedField[EnumType] {

  def this(rec: OwnerType, enum: EnumType, value: Option[EnumType#Value])(implicit m: Manifest[EnumType#Value]) = {
    this(rec, enum)
    setOption(value)
  }

  def owner = rec
  protected val valueManifest = m
}

