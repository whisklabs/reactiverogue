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

import reactiverogue.mongodb.JObjectParser
import net.liftweb.json._
import reactivemongo.bson._

class MongoCaseClassField[OwnerType <: BsonRecord[OwnerType], CaseType: Manifest](rec: OwnerType)
    extends Field[CaseType, OwnerType] with MandatoryTypedField[CaseType] {

  def owner = rec

  override def defaultValue = null.asInstanceOf[CaseType]
  override def isOptional = true

  override def asJValue: JValue = valueOpt.map(v => Extraction.decompose(v)).getOrElse(JNothing)

  override def setFromJValue(jvalue: JValue): Option[CaseType] = jvalue match {
    case JNothing | JNull => setOption(None)
    case s => setOption(s.extractOpt[CaseType])
  }

  def asBSONValue: BSONValue =
    JObjectParser.parse(asJValue.asInstanceOf[JObject])

  def setFromBSONValue(value: BSONValue): Option[CaseType] = {
    val jvalue = JObjectParser.serialize(value)
    setFromJValue(jvalue)
  }
}

