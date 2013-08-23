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

class MongoCaseClassListField[OwnerType <: BsonRecord[OwnerType], CaseType: Manifest](rec: OwnerType)
    extends Field[List[CaseType], OwnerType] with MandatoryTypedField[List[CaseType]] {

  def owner = rec

  override def defaultValue: List[CaseType] = Nil
  override def isOptional = true

  override def asJValue = JArray(value.map(v => Extraction.decompose(v)))

  override def setFromJValue(jvalue: JValue): Option[List[CaseType]] = jvalue match {
    case JArray(contents) =>
      setOption(Some(contents.flatMap(_.extractOpt[CaseType])))
    case _ => setOption(None)
  }

  def asBSONValue: BSONValue =
    JObjectParser.Parser.jValueToBSONValue(asJValue, _formats) match {
      case util.Success(v) => v
      case _ => BSONUndefined
    }

  def setFromBSONValue(value: BSONValue): Option[List[CaseType]] = {
    val jvalue = JObjectParser.serialize(value)
    setFromJValue(jvalue)
  }

}