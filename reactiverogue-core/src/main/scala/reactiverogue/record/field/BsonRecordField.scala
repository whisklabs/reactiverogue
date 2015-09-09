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

import reactivemongo.bson._

class BsonRecordField[OwnerType <: BsonRecord[OwnerType], SubRecordType <: BsonRecord[SubRecordType]](rec: OwnerType, valueMeta: BsonMetaRecord[SubRecordType])(implicit subRecordType: Manifest[SubRecordType])
    extends RequiredRecordField[SubRecordType, OwnerType] {

  def this(rec: OwnerType, valueMeta: BsonMetaRecord[SubRecordType], value: SubRecordType)(implicit subRecordType: Manifest[SubRecordType]) = {
    this(rec, value.meta)
    set(value)
  }

  def this(rec: OwnerType, valueMeta: BsonMetaRecord[SubRecordType], value: Option[SubRecordType])(implicit subRecordType: Manifest[SubRecordType]) = {
    this(rec, valueMeta)
    setOption(value)
  }

  def owner = rec
  def defaultValue = valueMeta.createRecord

  def asBSONValue: BSONValue =
    value.asBSONDocument

  def setFromBSONValue(value: BSONValue): Option[SubRecordType] = value match {
    case v: BSONDocument => setOption(Some(valueMeta.fromBSONDocument(v)))
    case _ => setOption(None)
  }
}
