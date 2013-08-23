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

import net.liftweb.json.JsonAST._
import net.liftweb.json.{ JsonParser, Printer }

//abstract class JsonObjectField[OwnerType <: BsonRecord[OwnerType], JObjectType <: JsonObject[JObjectType]]
//  (rec: OwnerType, valueMeta: JsonObjectMeta[JObjectType])
//  extends Field[JObjectType, OwnerType] with MandatoryTypedField[JObjectType] {
//
//  def owner = rec
//
//  implicit val formats = owner.meta.formats
//
//  /** Encode the field value into a JValue */
//  def asJValue: JValue = valueBox.map(_.asJObject) openOr (JNothing: JValue)
//
//  /*
//  * Decode the JValue and set the field to the decoded value.
//  * Returns Empty or Failure if the value could not be set
//  */
//  def setFromJValue(jvalue: JValue): Box[JObjectType] = jvalue match {
//    case JNothing|JNull if optional_? => setBox(Empty)
//    case o: JObject => setBox(tryo(valueMeta.create(o)))
//    case other => setBox(FieldHelpers.expectedA("JObject", other))
//  }
//
//  /*
//  * Convert this field's value into a DBObject so it can be stored in Mongo.
//  */
//  def asDBObject: DBObject = JObjectParser.parse(asJValue.asInstanceOf[JObject])
//
//  // set this field's value using a DBObject returned from Mongo.
//  def setFromDBObject(dbo: DBObject): Box[JObjectType] =
//    setFromJValue(JObjectParser.serialize(dbo).asInstanceOf[JObject])
//}

