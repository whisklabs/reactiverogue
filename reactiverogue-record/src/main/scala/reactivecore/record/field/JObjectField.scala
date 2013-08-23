/*
 * Copyright 2010-2012 WorldWide Conferencing, LLC
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
import reactiverogue.mongodb.JObjectParser

import reactivemongo.bson._

class JObjectField[OwnerType <: BsonRecord[OwnerType]](rec: OwnerType)
    extends Field[JObject, OwnerType] with MandatoryTypedField[JObject] {

  def owner = rec

  def defaultValue = JObject(List())

  override def asJValue = valueOpt.getOrElse(JNothing)

  override def setFromJValue(jvalue: JValue): Option[JObject] = jvalue match {
    case JNothing | JNull => setOption(None)
    case jo: JObject => setOption(Some(jo))
    case _ => setOption(None)
  }

  def asBSONValue: BSONValue =
    JObjectParser.parse(asJValue.asInstanceOf[JObject])(owner.meta.formats)

  def setFromBSONValue(value: BSONValue): Option[JObject] = {
    val jvalue = JObjectParser.serialize(value)(owner.meta.formats)
    setFromJValue(jvalue)
  }
}
