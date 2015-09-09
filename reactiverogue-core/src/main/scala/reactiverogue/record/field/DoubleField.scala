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

class DoubleField[OwnerType <: BsonRecord[OwnerType]](rec: OwnerType)
    extends DirectBsonField[Double] with RequiredRecordField[Double, OwnerType] {

  override def defaultValue: Double = null.asInstanceOf[Double]

  def this(rec: OwnerType, value: Double) = {
    this(rec)
    set(value)
  }

  def owner = rec
}

class OptionalDoubleField[OwnerType <: BsonRecord[OwnerType]](rec: OwnerType)
    extends DirectBsonField[Double] with OptionalRecordField[Double, OwnerType] {

  def this(rec: OwnerType, value: Option[Double]) = {
    this(rec)
    setOption(value)
  }

  def owner = rec
}

