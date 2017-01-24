package reactiverogue.record
package field

import reactivemongo.bson._
import play.api.libs.json.{JsObject, Format}
import reactivemongo.play.json.BSONFormats

class JsObjectListField[OwnerType <: BsonRecord[OwnerType], T: Format](rec: OwnerType)
    extends RequiredRecordField[List[T], OwnerType] {

  def owner = rec

  override def defaultValue: List[T] = Nil
  override def isOptional = true

  def valueToBsonValue(v: T): BSONValue = {
    BSONFormats.BSONDocumentFormat.reads(implicitly[Format[T]].writes(v)).get
  }

  def asBSONValue: BSONValue =
    valueOpt
      .map(v => BSONArray(v.map(valueToBsonValue)))
      .getOrElse(BSONArray())

  def setFromBSONValue(value: BSONValue): Option[List[T]] = {
    value match {
      case BSONArray(arr) =>
        val res = arr.toList.flatMap {
          case scala.util.Success(d: BSONDocument) =>
            implicitly[Format[T]]
              .reads(BSONFormats.BSONDocumentFormat.writes(d).as[JsObject])
              .asOpt
          case _ =>
            None
        }
        setOption(Some(res))
      case _ => setOption(None)
    }
  }

}
