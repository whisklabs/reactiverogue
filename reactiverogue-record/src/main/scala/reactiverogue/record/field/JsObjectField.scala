/*
* Copyright 2010-2012 WorldWide Conferencing, LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package reactiverogue.record
package field

import play.api.libs.json._
import play.modules.reactivemongo.json.BSONFormats
import reactivemongo.bson.{ BSONDocument, BSONUndefined, BSONValue }

abstract class JsObjectField[OwnerType <: BsonRecord[OwnerType], JObjectType: Format](rec: OwnerType)
    extends Field[JObjectType, OwnerType] with OptionalTypedField[JObjectType] {

  def owner = rec

  override def asBSONValue: BSONValue =
    valueOpt.map(v =>
      BSONFormats.BSONDocumentFormat.reads(implicitly[Format[JObjectType]].writes(v)).get).getOrElse(BSONUndefined)

  override def setFromBSONValue(value: BSONValue): Option[JObjectType] = {
    value match {
      case d: BSONDocument => setOption(implicitly[Format[JObjectType]].reads(BSONFormats.BSONDocumentFormat.writes(d).as[JsObject]).asOpt)
      case _ => setOption(None)
    }

  }
}

