/*
 * Copyright 2010-2011 WorldWide Conferencing, LLC
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

import reactivemongo.core.commands.{ GetLastError, LastError }
import reactiverogue.record.field.ObjectIdPk
import concurrent.{ ExecutionContext, Future }

trait MongoRecord[MyType <: MongoRecord[MyType]] extends BsonRecord[MyType] with ObjectIdPk[MyType] {
  self: MyType =>

  /**
   * The meta record (the object that contains the meta result for this type)
   */
  def meta: MongoMetaRecord[MyType]

  /**
   * Save the instance and return the instance
   */
  def save(concern: GetLastError)(implicit ec: ExecutionContext): Future[LastError] = {
    meta.save(this, concern)
  }

  /**
   * Save the instance and return the instance
   * @param safe - if true will use GetLastError SAFE else NORMAL
   */
  def save(safe: Boolean)(implicit ec: ExecutionContext): Future[LastError] = {
    save(if (safe) GetLastError(j = true) else GetLastError())
  }

  /**
   * Save the instance and return the instance
   * WILL NOT RAISE MONGO SERVER ERRORS.
   * Use save(Boolean) or save(GetLastError) to control error behavior
   */
  def save(implicit ec: ExecutionContext): Future[LastError] = save(false)

  /**
   * Delete the instance from backing store
   */
  def delete_!(implicit ec: ExecutionContext): Future[LastError] =
    meta.delete_!(this)
}
