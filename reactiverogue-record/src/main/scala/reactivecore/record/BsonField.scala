/*
 * Copyright 2007-2012 WorldWide Conferencing, LLC
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

import reactivemongo.bson._
import reactiverogue.mongodb.BSONSerializable

/** Base trait of bson record fields */
trait BsonField[T] {

  type ValueType

  private[record] var fieldName: String = _
  var data: Option[T] = None

  def toValueType(in: Option[T]): ValueType

  def toOptionValue(in: ValueType): Option[T]

  def set(value: ValueType): ValueType

  def setOption(value: Option[T]): Option[T] = {
    data = value
    data
  }

  /**
   * The text name of this field
   */
  def name: String = fieldName

  private[record] final def setName_!(newName: String): String = {
    fieldName = newName
    fieldName
  }

  def defaultValue: ValueType

  def isOptional: Boolean

  /** Encode the field value into a BSONValue */
  def asBSONValue: BSONValue

  def setFromBSONValue(value: BSONValue): Option[T]

  def value: ValueType

  def valueOpt: Option[T] = data
}

/** Refined trait for fields owned by a particular record type */
trait OwnedField[T, OwnerType <: BsonRecord[OwnerType]] extends BsonField[T] {
  /**
   * Return the owner of this field
   */
  def owner: OwnerType
}

abstract class DirectBsonField[T: BSONSerializable] extends BsonField[T] {

  def asBSONValue: BSONValue = {
    val opt = if (isOptional || valueOpt.isDefined) valueOpt else setOption(toOptionValue(defaultValue))
    opt.map(BSONSerializable[T].asBSONValue).getOrElse(BSONUndefined)
  }

  def setFromBSONValue(v: BSONValue): Option[T] = {
    val pf = BSONSerializable[T].fromBSONValue
    if (pf.isDefinedAt(v)) setOption(Some(pf(v)))
    else setOption(None)
  }

}

trait MandatoryTypedField[T] extends BsonField[T] {

  type ValueType = T

  override def set(in: T): T = setOption(Some(in)).getOrElse(defaultValue)

  def toValueType(in: Option[T]): ValueType = in.getOrElse(defaultValue)

  def toOptionValue(in: ValueType): Option[T] = Option(in)

  def value: T = valueOpt.getOrElse(defaultValue)

  /**
   * The default value of the field when a field has no value set and is optional, or a method that must return a value (e.g. value) is used
   */
  def defaultValue: ValueType

  def isOptional: Boolean = false
}

trait OptionalTypedField[T] extends BsonField[T] {

  /**
   * ValueType represents the type that users will work with.  For OptionalTypedField, this is
   * equal to Option[ThisType].
   */
  type ValueType = Option[T]

  override def set(in: Option[T]): Option[T] = setOption(in).orElse(defaultValue)

  def toValueType(in: Option[T]) = in.filterNot(_ != null)

  def toOptionValue(in: ValueType) = in

  def value: ValueType = valueOpt

  def defaultValue: ValueType = None

  def isOptional: Boolean = true
}

/**
 * A simple field that can store and retrieve a value of a given type
 */
trait Field[T, OwnerType <: BsonRecord[OwnerType]] extends OwnedField[T, OwnerType] with BsonField[T] {

  def apply(in: T): OwnerType = apply(Some(in))

  def apply(in: Option[T]): OwnerType = {
    setOption(in)
    owner
  }
}
