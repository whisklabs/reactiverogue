/*
* Copyright 2010-2013 WorldWide Conferencing, LLC
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

import java.util.Date
import net.liftweb.json._
import reactivemongo.bson._
import reactiverogue.mongodb.JObjectParser

trait DateTypedField extends BsonField[Date] {

  override def setFromJValue(jvalue: JValue): Option[Date] = jvalue match {
    case JNothing | JNull => setOption(None)
    case JObject(JField("$dt", JString(s)) :: Nil) =>
      setOption(formats.dateFormat.parse(s))
    case other => setOption(None)
  }

  override def asJValue: JValue = valueOpt.map(v =>
    reactiverogue.mongodb.Meta.dateAsJValue(v, formats)).getOrElse(JNothing)

  def asBSONValue: BSONValue =
    JObjectParser.Parser.jValueToBSONValue(asJValue, formats) match {
      case util.Success(v) => v
      case _ => BSONUndefined
    }

  def setFromBSONValue(value: BSONValue): Option[Date] = {
    val jvalue = JObjectParser.serialize(value)(formats)
    setFromJValue(jvalue)
  }
}

class DateField[OwnerType <: BsonRecord[OwnerType]](rec: OwnerType)
    extends Field[Date, OwnerType] with MandatoryTypedField[Date] with DateTypedField {

  def owner = rec

  override def formats = owner.meta.formats

  def this(rec: OwnerType, value: Date) = {
    this(rec)
    setOption(Some(value))
  }

  def defaultValue = new Date

  override def toString = value match {
    case null => "null"
    case d => valueOpt.map(formats.dateFormat.format).getOrElse("")
  }
}

class OptionalDateField[OwnerType <: BsonRecord[OwnerType]](rec: OwnerType)
    extends Field[Date, OwnerType] with OptionalTypedField[Date] with DateTypedField {

  def owner = rec

  override def formats = owner.meta.formats

  def this(rec: OwnerType, value: Option[Date]) = {
    this(rec)
    setOption(value)
  }
}
