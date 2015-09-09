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
import reactivemongo.bson._

trait DateTypedField extends BsonField[Date] {

  def asBSONValue: BSONValue =
    valueOpt.map(v => BSONDateTime(v.getTime)).getOrElse(BSONUndefined)

  def setFromBSONValue(value: BSONValue): Option[Date] =
    value match {
      case BSONDateTime(v) => setOption(Some(new java.util.Date(v)))
      case _ => setOption(None)
    }
}

class DateField[OwnerType <: BsonRecord[OwnerType]](rec: OwnerType)
    extends RequiredRecordField[Date, OwnerType] with DateTypedField {

  def owner = rec

  def this(rec: OwnerType, value: Date) = {
    this(rec)
    setOption(Some(value))
  }

  def defaultValue = new Date
}

class OptionalDateField[OwnerType <: BsonRecord[OwnerType]](rec: OwnerType)
    extends OptionalRecordField[Date, OwnerType] with DateTypedField {

  def owner = rec

  def this(rec: OwnerType, value: Option[Date]) = {
    this(rec)
    setOption(value)
  }
}
