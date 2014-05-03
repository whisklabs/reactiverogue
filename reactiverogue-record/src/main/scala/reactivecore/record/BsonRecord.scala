/*
 * Copyright 2011-2013 WorldWide Conferencing, LLC
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
import java.util.regex.Pattern
import scala.collection.JavaConversions._

import reactivemongo.bson._
import java.lang.reflect.Method
import scala.language.existentials

/** Specialized Record that can be encoded and decoded from BSON (DBObject) */
trait BsonRecord[MyType <: BsonRecord[MyType]] {
  self: MyType =>

  /**
   * Get the fields defined on the meta object for this record instance
   */
  def fields() = meta.fields(this)

  def allFields = fields()

  /** Refines meta to require a BsonMetaRecord */
  def meta: BsonMetaRecord[MyType]

  /**
   * Encode a record instance into a DBObject
   */
  def asBSONDocument: BSONDocument = meta.asBSONDocument(this)

  /**
   * Set the fields of this record from the given DBObject
   */
  def setFieldsFromBSONDocument(dbo: BSONDocument): Unit = meta.setFieldsFromBSONDocument(this, dbo)

  //  def asJValue: JValue =
  //    JObjectParser.serialize(asBSONDocument)(meta.formats)
  //
  //  def setFieldsFromJValue(jvalue: JValue): Unit =
  //    setFieldsFromBSONDocument(JObjectParser.parse(asJValue.asInstanceOf[JObject])(meta.formats))

  def fieldByName(fieldName: String): Option[Field[_, MyType]] = meta.fieldByName(fieldName, this)

  override def equals(other: Any): Boolean = {
    other match {
      case that: BsonRecord[MyType] =>
        that.fields.corresponds(this.fields) { (a, b) =>
          a.name == b.name && a.valueOpt == b.valueOpt
        }
      case _ => false
    }
  }

  def copy: MyType = meta.copy(this)

}

/** Specialized MetaRecord that deals with BsonRecords */
trait BsonMetaRecord[BaseRecord <: BsonRecord[BaseRecord]] {
  self: BaseRecord =>

  private var fieldList: List[FieldHolder] = Nil
  private var fieldMap: Map[String, FieldHolder] = Map.empty

  /**
   * Defines the order of the fields in this record
   *
   * @return a List of Field
   */
  def fieldOrder: List[Field[_, BaseRecord]] = Nil

  protected val rootClass = this.getClass.getSuperclass

  private def isField(m: Method) = {
    val ret = !m.isSynthetic && classOf[Field[_, _]].isAssignableFrom(m.getReturnType)
    ret
  }

  def introspect(rec: BaseRecord, methods: Array[Method])(f: (Method, Field[_, BaseRecord]) => Any): Unit = {

    // find all the potential fields
    val potentialFields = methods.toList.filter(isField)

    // any fields with duplicate names get put into a List
    val map: Map[String, List[Method]] = potentialFields.foldLeft[Map[String, List[Method]]](Map()) {
      case (map, method) =>
        val name = method.getName
        map + (name -> (method :: map.getOrElse(name, Nil)))
    }

    // sort each list based on having the most specific type and use that method
    val realMeth = map.values.map(_.sortWith {
      case (a, b) => !a.getReturnType.isAssignableFrom(b.getReturnType)
    }).map(_.head)

    for (v <- realMeth) {
      v.invoke(rec) match {
        case mf: Field[_, BaseRecord] =>
          mf.setName_!(v.getName)
          f(v, mf)
        case _ =>
      }
    }

  }

  synchronized { //TODO: see Safe.runSafe
    val tArray = new collection.mutable.ListBuffer[FieldHolder]

    val methods = rootClass.getMethods

    introspect(this, methods) {
      case (v, mf) => tArray += FieldHolder(mf.name, v, mf)
    }

    fieldList = {
      val ordered = fieldOrder.flatMap(f => tArray.find(_.metaField == f))
      ordered ++ (tArray -- ordered)
    }

    fieldMap = Map() ++ fieldList.map(i => (i.name, i))
  }

  /**
   * Creates a new record
   */
  def createRecord: BaseRecord = {
    val rec = instantiateRecord
    fieldList.foreach(fh => fh.field(rec).setName_!(fh.name))
    rec
  }

  /** Make a new record instance. This method can be overridden to provide caching behavior or what have you. */
  protected def instantiateRecord: BaseRecord = rootClass.newInstance.asInstanceOf[BaseRecord]

  /**
   * Get a field by the field name
   * @param fieldName -- the name of the field to get
   * @param actual -- the instance to get the field on
   *
   * @return Box[The Field] (Empty if the field is not found)
   */
  def fieldByName(fieldName: String, inst: BaseRecord): Option[Field[_, BaseRecord]] = {
    fieldMap.get(fieldName).map(_.field(inst))
  }

  /**
   * Populate the fields of the record with values from an existing record
   *
   * @param inst - The record to populate
   * @param rec - The Record to read from
   */
  def setFieldsFromRecord(inst: BaseRecord, rec: BaseRecord) {
    for {
      fh <- fieldList
      fld <- rec.fieldByName(fh.name)
    } {
      fh.field(inst).setFromBSONValue(fld.asBSONValue)
    }
  }

  def copy(rec: BaseRecord): BaseRecord = {
    val inst = createRecord
    setFieldsFromRecord(inst, rec)
    inst
  }

  /**
   * Renamed from fields() due to a clash with fields() in Record. Use this method
   * to obtain a list of fields defined in the meta companion objects. Possibly a
   * breaking change? (added 14th August 2009, Tim Perrett)
   *
   * @see Record
   */
  def metaFields(): List[Field[_, BaseRecord]] = fieldList.map(_.metaField)

  /**
   * Obtain the fields for a particular Record or subclass instance by passing
   * the instance itself.
   * (added 14th August 2009, Tim Perrett)
   */
  def fields(rec: BaseRecord): List[Field[_, BaseRecord]] = fieldList.map(_.field(rec))

  /**
   * Create a BSONDocument from the field names and values.
   * - MongoFieldFlavor types (List) are converted to BSONDocuments
   *   using asBSONDocument
   */
  def asBSONDocument(inst: BaseRecord): BSONDocument = {

    val fieldList =
      for {
        field <- fields(inst)
        dbValue <- fieldDbValue(field)
        if dbValue != BSONUndefined
      } yield {
        field.name -> dbValue
      }

    BSONDocument(fieldList)
  }

  def fieldDbValue(f: Field[_, BaseRecord]): Option[BSONValue] = {
    if (f.isOptional && f.valueOpt.isEmpty) None
    else Some(f.asBSONValue)
  }

  /**
   * Creates a new record, then sets the fields with the given BSONDocument.
   *
   * @param document - the BSONDocument
   * @return Box[BaseRecord]
   */
  def fromBSONDocument(document: BSONDocument): BaseRecord = {
    val inst: BaseRecord = createRecord
    setFieldsFromBSONDocument(inst, document)
    inst
  }

  //  def fromJValue(value: JValue): BaseRecord = {
  //    val inst: BaseRecord = createRecord
  //    inst.setFieldsFromJValue(value)
  //    inst
  //  }

  /**
   * Populate the inst's fields with the values from a BSONDocument. Values are set
   * using setFromAny passing it the BSONDocument returned from Mongo.
   *
   * @param inst - the record that will be populated
   * @param document - The BSONDocument
   * @return Unit
   */
  def setFieldsFromBSONDocument(inst: BaseRecord, document: BSONDocument): Unit = {
    for {
      (f, v) <- document.elements
      field <- inst.fieldByName(f)
    } {
      field.setFromBSONValue(v)
    }
  }

  case class FieldHolder(name: String, method: Method, metaField: Field[_, BaseRecord]) {
    def field(inst: BaseRecord): Field[_, BaseRecord] = method.invoke(inst).asInstanceOf[Field[_, BaseRecord]]
  }
}
