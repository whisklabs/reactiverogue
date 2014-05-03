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

import reactivemongo.bson._

trait StringTypedField extends DirectBsonField[String]

class StringField[OwnerType <: BsonRecord[OwnerType]](rec: OwnerType)
    extends DirectBsonField[String] with Field[String, OwnerType] with MandatoryTypedField[String] {

  override def defaultValue = null.asInstanceOf[String]

  def this(rec: OwnerType, value: String) = {
    this(rec)
    set(value)
  }

  def owner = rec
}

class OptionalStringField[OwnerType <: BsonRecord[OwnerType]](rec: OwnerType)
    extends DirectBsonField[String] with Field[String, OwnerType] with OptionalTypedField[String] {

  def this(rec: OwnerType, value: Option[String]) = {
    this(rec)
    setOption(value)
  }

  def owner = rec
}

