/*
* Copyright 2010-2011 WorldWide Conferencing, LLC
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

import play.modules.reactivemongo.json.BSONFormats
import reactivemongo.bson._
import play.api.libs.json.{ JsObject, Format }

class JsObjectListField[OwnerType <: BsonRecord[OwnerType], T: Format](rec: OwnerType)
    extends Field[List[T], OwnerType] with MandatoryTypedField[List[T]] {

  def owner = rec

  override def defaultValue: List[T] = Nil
  override def isOptional = true

  def valueToBsonValue(v: T): BSONValue = {
    BSONFormats.BSONDocumentFormat.reads(implicitly[Format[T]].writes(v)).get
  }

  def asBSONValue: BSONValue =
    valueOpt.map(v =>
      BSONArray(v.map(valueToBsonValue)))
      .getOrElse(BSONArray())

  def setFromBSONValue(value: BSONValue): Option[List[T]] = {
    value match {
      case BSONArray(arr) =>
        val res = arr.toList.flatMap {
          case scala.util.Success(d: BSONDocument) =>
            implicitly[Format[T]].reads(BSONFormats.BSONDocumentFormat.writes(d).as[JsObject]).asOpt
          case _ =>
            None
        }
        setOption(Some(res))
      case _ => setOption(None)
    }
  }

}