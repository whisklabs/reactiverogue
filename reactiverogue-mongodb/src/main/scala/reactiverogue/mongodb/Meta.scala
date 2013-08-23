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

package reactiverogue.mongodb

import net.liftweb.json._

import java.util.{ Calendar, Date, GregorianCalendar, UUID }
import java.util.regex.Pattern

import reactivemongo.bson.BSONObjectID
import org.joda.time._

import JsonDSL._

object Meta {

  def dateAsJValue(d: Date, formats: Formats): JValue = ("$dt" -> formats.dateFormat.format(d))
  def timestampAsJValue(ts: Long, formats: Formats): JValue = ("$ts" -> formats.dateFormat.format(new Date(ts)))
  def objectIdAsJValue(oid: BSONObjectID): JValue = ("$oid" -> oid.stringify)
  def patternAsJValue(p: Pattern): JValue = ("$regex" -> p.pattern) ~ ("$flags" -> p.flags)
  def uuidAsJValue(u: UUID): JValue = ("$uuid" -> u.toString)

  def objectIdAsJValue(oid: BSONObjectID, formats: Formats): JValue =
    if (isObjectIdSerializerUsed(formats))
      objectIdAsJValue(oid)
    else
      JString(oid.stringify)

  /*
  * Check to see if the ObjectIdSerializer is being used.
  */
  private def isObjectIdSerializerUsed(formats: Formats): Boolean =
    formats.customSerializers.exists(_.getClass == objectIdSerializerClass)

  private val objectIdSerializerClass = classOf[reactiverogue.mongodb.ObjectIdSerializer]
}

