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

import reactiverogue.mongodb._
import reactivemongo.api.commands.{ WriteResult, WriteConcern }
import reactivemongo.bson._
import concurrent.{ ExecutionContext, Future }

trait MongoMetaRecord[BaseRecord <: MongoRecord[BaseRecord]]
    extends BsonMetaRecord[BaseRecord] with MongoMeta[BaseRecord] {

  self: BaseRecord =>

  /**
   * Delete the instance from backing store
   */
  def delete_!(inst: BaseRecord)(implicit ec: ExecutionContext): Future[WriteResult] = {
    useColl(coll =>
      coll.remove(BSONDocument("_id" -> inst.id.value), WriteConcern.Default, true))
  }

  def bulkDelete_!!(qry: BSONDocument)(implicit ec: ExecutionContext): Future[WriteResult] = {
    useColl(coll =>
      coll.remove(qry, WriteConcern.Default, false))
  }

  /**
   * Save the instance in the appropriate backing store
   */
  def save(inst: BaseRecord, concern: WriteConcern)(implicit ec: ExecutionContext): Future[WriteResult] = {
    useColl(coll =>
      coll.insert(inst.asBSONDocument, concern))
  }

}
