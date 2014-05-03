/*
 * Copyright 2010-2013 WorldWide Conferencing, LLC
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

import reactiverogue.mongodb.BSONSerializable

class MongoMapField[OwnerType <: BsonRecord[OwnerType], MapValueType: BSONSerializable](rec: OwnerType)
    extends DirectBsonField[Map[String, MapValueType]] with Field[Map[String, MapValueType], OwnerType]
    with MandatoryTypedField[Map[String, MapValueType]] {

  def owner = rec

  def defaultValue = Map[String, MapValueType]()

}

