package reactiverogue.record
package field

import play.api.libs.json._
import reactivemongo.bson.{ BSONDocument, BSONUndefined, BSONValue }
import reactiverogue.json.BSONFormats

abstract class JsObjectField[OwnerType <: BsonRecord[OwnerType], JObjectType: Format](rec: OwnerType)
    extends OptionalRecordField[JObjectType, OwnerType] {

  def owner = rec

  def valueToBsonValue(v: JObjectType): BSONDocument = {
    BSONFormats.BSONDocumentFormat.reads(implicitly[Format[JObjectType]].writes(v)).get
  }

  override def asBSONValue: BSONValue =
    valueOpt.map(valueToBsonValue).getOrElse(BSONUndefined)

  override def setFromBSONValue(value: BSONValue): Option[JObjectType] = {
    value match {
      case d: BSONDocument => setOption(implicitly[Format[JObjectType]].reads(BSONFormats.BSONDocumentFormat.writes(d).as[JsObject]).asOpt)
      case _ => setOption(None)
    }

  }
}

