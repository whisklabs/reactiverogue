package reactiverogue.core

import play.api.libs.json.Json

object JsonFormats {

  implicit val latLongFormat = Json.format[LatLong]
  implicit val commentFormat = Json.format[OneComment]

}
