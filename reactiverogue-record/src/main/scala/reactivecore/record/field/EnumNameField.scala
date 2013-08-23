/*
 * Copyright 2007-2011 WorldWide Conferencing, LLC
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

trait EnumNameTypedField[EnumType <: Enumeration] extends BsonField[EnumType#Value] {
  protected val enum: EnumType
  protected val valueManifest: Manifest[EnumType#Value]

  override def asJValue: JValue = valueOpt.map(v => JString(v.toString)).getOrElse(JNothing)

  override def setFromJValue(jvalue: JValue): Option[EnumType#Value] = jvalue match {
    case JNothing | JNull => setOption(None)
    case JString(s) => setOption(enum.values.find(_.toString == s))
    case other => setOption(None)
  }

  def asBSONValue: BSONValue =
    valueOpt.map(v => BSONString(v.toString)).getOrElse(BSONUndefined)

  def setFromBSONValue(value: BSONValue): Option[EnumType#Value] = value match {
    case BSONString(s) => setOption(enum.values.find(_.toString == s))
    case _ => setOption(None)
  }
}

class EnumNameField[OwnerType <: BsonRecord[OwnerType], EnumType <: Enumeration](rec: OwnerType, protected val enum: EnumType)(implicit m: Manifest[EnumType#Value])
    extends Field[EnumType#Value, OwnerType] with MandatoryTypedField[EnumType#Value] with EnumNameTypedField[EnumType] {
  def this(rec: OwnerType, enum: EnumType, value: EnumType#Value)(implicit m: Manifest[EnumType#Value]) = {
    this(rec, enum)
    set(value)
  }

  def defaultValue: EnumType#Value = enum.values.iterator.next

  def owner = rec
  protected val valueManifest = m
}

class OptionalEnumNameField[OwnerType <: BsonRecord[OwnerType], EnumType <: Enumeration](rec: OwnerType, protected val enum: EnumType)(implicit m: Manifest[EnumType#Value])
    extends Field[EnumType#Value, OwnerType] with OptionalTypedField[EnumType#Value] with EnumNameTypedField[EnumType] {
  def this(rec: OwnerType, enum: EnumType, value: Option[EnumType#Value])(implicit m: Manifest[EnumType#Value]) = {
    this(rec, enum)
    setOption(value)
  }

  def owner = rec
  protected val valueManifest = m
}

