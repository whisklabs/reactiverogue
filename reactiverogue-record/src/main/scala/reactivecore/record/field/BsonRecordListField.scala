/*
 * Copyright 2011 WorldWide Conferencing, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactiverogue.record
package field

import net.liftweb.json._
import reactivemongo.bson._

/*
 * List of BsonRecords
 */
class BsonRecordListField[OwnerType <: BsonRecord[OwnerType], SubRecordType <: BsonRecord[SubRecordType]](rec: OwnerType, valueMeta: BsonMetaRecord[SubRecordType])(implicit mf: Manifest[SubRecordType])
    extends Field[List[SubRecordType], OwnerType] with MandatoryTypedField[List[SubRecordType]] {

  def owner: OwnerType = rec

  override def defaultValue: List[SubRecordType] = Nil
  override def isOptional = true

  override def asJValue = JArray(value.map(_.asJValue))

  override def setFromJValue(jvalue: JValue) = jvalue match {
    case JNothing | JNull => setOption(None)
    case JArray(arr) => setOption(Some(arr.map(valueMeta.fromJValue)))
    case other => setOption(None)
  }

  def asBSONValue: BSONValue =
    BSONArray(value.map(_.asBSONDocument))

  def setFromBSONValue(value: BSONValue): Option[List[SubRecordType]] = value match {
    case x: BSONArray => setOption(Some(x.values.collect({ case v: BSONDocument => valueMeta.fromBSONDocument(v) }).toList))
    case _ => setOption(None)
  }

}
