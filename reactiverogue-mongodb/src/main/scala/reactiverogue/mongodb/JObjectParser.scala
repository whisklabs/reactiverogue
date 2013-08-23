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

import scala.collection.JavaConversions._

import java.util.{ Date, UUID }
import java.util.regex.Pattern

import net.liftweb.json._

import reactivemongo.bson._

import scala.util.{ Try, Success, Failure }

object JObjectParser {

  /*
  * Parse a JObject into a BSONDocument
  */
  def parse(jo: JObject)(implicit formats: Formats): BSONDocument =
    Parser.parse(jo, formats)

  /*
  * Serialize a BSONDocument into a JObject
  */
  def serialize(a: BSONValue)(implicit formats: Formats): JValue = serialize(a, formats)

  private def serialize(a: BSONValue, formats: Formats): JValue = {
    a match {
      case null => JNull
      case x: BSONObjectID => Meta.objectIdAsJValue(x, formats)
      case BSONBoolean(v) => JBool(v)
      case BSONDateTime(v) => Meta.dateAsJValue(new Date(v), formats)
      case BSONDouble(v) => JDouble(v)
      case BSONInteger(v) => JInt(v)
      case BSONLong(v) => JInt(v.toInt)
      case BSONNull => JNull
      case BSONString(v) => JString(v)
      case BSONUndefined => JNothing
      case BSONSymbol(v) => JString(v)
      case BSONRegex(str, flags) => JObject(JField("$regex", JString(str)) :: JField("$options", JString(flags)) :: Nil)
      case BSONTimestamp(v) => Meta.timestampAsJValue(v, formats)
      case x: BSONArray => JArray(x.values.map(x => serialize(x, formats)).toList)
      case x: BSONDocument => JObject(
        x.elements.map {
          case (name, value) =>
            JField(name, serialize(value, formats))
        } toList)
      case x => {
        JNothing
      }
    }
  }

  object Parser {

    def parse(jo: JObject, formats: Formats): BSONDocument = {
      parseObject(jo.obj, formats)
    }

    def jValueToBSONValue(jv: JValue, formats: Formats): Try[BSONValue] = jv match {
      case JObject(JField("$oid", JString(s)) :: Nil) if BSONObjectID.parse(s).isSuccess =>
        Try(BSONObjectID(s))
      case JObject(JField("$regex", JString(s)) :: JField("$options", JString(f)) :: Nil) =>
        Try(BSONRegex(s, f))
      case JObject(JField("$dt", JString(s)) :: Nil) =>
        formats.dateFormat.parse(s) match {
          case Some(date) => Try(BSONDateTime(date.getTime))
          case None => Failure(new Exception("parsing error"))
        }
      case JObject(JField("$ts", JString(s)) :: Nil) =>
        formats.dateFormat.parse(s) match {
          case Some(date) => Try(BSONTimestamp(date.getTime))
          case None => Failure(new Exception("parsing error"))
        }
      case JArray(arr) =>
        Try(parseArray(arr, formats))
      case JObject(jo) =>
        Try(parseObject(jo, formats))
      case JBool(b) =>
        Success(BSONBoolean(b))
      case JInt(n) =>
        renderInteger(n)
      case JDouble(n) =>
        Success(BSONDouble(n))
      case JNull =>
        Success(BSONNull)
      case JString(null) =>
        Success(BSONNull)
      case JString(s) =>
        Success(BSONString(s))
      case x =>
        Failure(new Exception("unknown type: " + x))
    }

    private def parseArray(arr: List[JValue], formats: Formats): BSONArray = {
      val list: List[Try[BSONValue]] = arr.withFilter(_ != JNothing).map(v => jValueToBSONValue(v, formats))
      BSONArray(list.toStream)
    }

    private def parseObject(obj: List[JField], formats: Formats): BSONDocument = {

      val fields: List[Try[(String, BSONValue)]] = obj.withFilter(_.value != JNothing).map {
        case JField(f, value) =>
          jValueToBSONValue(value, formats).map(f -> _)
      }
      BSONDocument(fields.toStream)
    }

    private def renderInteger(i: BigInt): Try[BSONValue] = {
      if (i <= java.lang.Integer.MAX_VALUE && i >= java.lang.Integer.MIN_VALUE) {
        Success(BSONInteger(i.intValue))
      } else if (i <= java.lang.Long.MAX_VALUE && i >= java.lang.Long.MIN_VALUE) {
        Success(BSONLong(i.longValue))
      } else {
        Failure(new Exception("BigInt parsing exception"))
      }
    }

  }
}

