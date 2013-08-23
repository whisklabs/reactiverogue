// Copyright 2012 Foursquare Labs Inc. All Rights Reserved.

package reactiverogue.core

import reactiverogue.core.MongoHelpers.MongoSelect
import reactiverogue.record.{ BsonRecord, BsonMetaRecord, MongoRecord, MongoMetaRecord }
import reactiverogue.mongodb.MongoDB
import reactiverogue.record.field.BsonRecordField
import reactivemongo.bson._
import reactivemongo.api._
import reactivemongo.api.collections.default._

object LiftDBCollectionFactory extends DBCollectionFactory[MongoRecord[_] with MongoMetaRecord[_]] {
  override def getDBCollection[M <: MongoRecord[_] with MongoMetaRecord[_]](query: Query[M, _, _]): BSONCollection = {
    MongoDB.useSession(query.meta.mongoIdentifier) { db =>
      db(query.collectionName)
    }
  }
  override def getPrimaryDBCollection[M <: MongoRecord[_] with MongoMetaRecord[_]](query: Query[M, _, _]): BSONCollection = {
    MongoDB.useSession(query.meta /* TODO: .master*/ .mongoIdentifier) { db =>
      db(query.collectionName)
    }
  }
  override def getInstanceName[M <: MongoRecord[_] with MongoMetaRecord[_]](query: Query[M, _, _]): String = {
    query.meta.mongoIdentifier.toString
  }
}

class LiftAdapter(dbCollectionFactory: DBCollectionFactory[MongoRecord[_] with MongoMetaRecord[_]])
  extends ReactiveMongoAdapter(dbCollectionFactory)

object LiftAdapter extends LiftAdapter(LiftDBCollectionFactory)

class LiftQueryExecutor(override val adapter: ReactiveMongoAdapter[MongoRecord[_] with MongoMetaRecord[_]]) extends QueryExecutor[MongoRecord[_] with MongoMetaRecord[_]] {
  override def defaultWriteConcern = QueryHelpers.config.defaultWriteConcern
  override lazy val optimizer = new QueryOptimizer

  override protected def serializer[M <: MongoRecord[_] with MongoMetaRecord[_], R](
    meta: M,
    select: Option[MongoSelect[M, R]]): RogueSerializer[R] = {
    new RogueSerializer[R] {
      override def fromBSONDocument(dbo: BSONDocument): R = select match {
        case Some(MongoSelect(Nil, transformer)) =>
          // A MongoSelect clause exists, but has empty fields. Return null.
          // This is used for .exists(), where we just want to check the number
          // of returned results is > 0.
          transformer(null)
        case Some(MongoSelect(fields, transformer)) =>
          val inst = meta.createRecord.asInstanceOf[MongoRecord[_]]

          LiftQueryExecutorHelpers.setInstanceFieldFromDbo(inst, dbo, "_id")

          val values =
            fields.map(fld => {
              val valueOpt = LiftQueryExecutorHelpers.setInstanceFieldFromDbo(inst, dbo, fld.field.name)
              fld.valueOrDefault(valueOpt)
            })

          transformer(values)
        case None =>
          meta.fromBSONDocument(dbo).asInstanceOf[R]
      }
    }
  }
}

object LiftQueryExecutor extends LiftQueryExecutor(LiftAdapter)

object LiftQueryExecutorHelpers {
  import reactiverogue.record.{ Field => LField }

  def setInstanceFieldFromDboList(instance: BsonRecord[_], doc: BSONDocument, fieldNames: List[String]): Option[_] = {
    fieldNames match {
      case last :: Nil =>
        val fld: Option[LField[_, _]] = instance.fieldByName(last)
        fld.flatMap(setLastFieldFromDbo(_, doc, last))
      case name :: rest =>
        val fld: Option[LField[_, _]] = instance.fieldByName(name)
        doc.get(name) match {
          case Some(obj: BSONDocument) => fld.flatMap(setFieldFromDbo(_, obj, rest))
          case Some(list: BSONArray) => fallbackValueFromDbObject(doc, fieldNames)
          case _ => None
        }
      case Nil => throw new UnsupportedOperationException("was called with empty list, shouldn't possibly happen")
    }
  }

  def setFieldFromDbo(field: LField[_, _], dbo: BSONDocument, fieldNames: List[String]): Option[_] = {
    if (field.isInstanceOf[BsonRecordField[_, _]]) {
      val brf = field.asInstanceOf[BsonRecordField[_, _]]
      val inner = brf.value.asInstanceOf[BsonRecord[_]]
      setInstanceFieldFromDboList(inner, dbo, fieldNames)
    } else {
      fallbackValueFromDbObject(dbo, fieldNames)
    }
  }

  def setLastFieldFromDbo(field: LField[_, _], dbo: BSONDocument, fieldName: String): Option[_] = {
    dbo.get(fieldName).flatMap(field.setFromBSONValue)
  }

  def setInstanceFieldFromDbo(instance: MongoRecord[_], dbo: BSONDocument, fieldName: String): Option[_] = {
    fieldName.contains(".") match {
      case true =>
        val names = fieldName.split("\\.").toList
        setInstanceFieldFromDboList(instance, dbo, names)
      case false =>
        val fld: Option[LField[_, _]] = instance.fieldByName(fieldName)
        fld.flatMap(setLastFieldFromDbo(_, dbo, fieldName))
    }
  }

  def bsonValueToAnyRef: PartialFunction[BSONValue, AnyRef] = {
    case BSONInteger(v) => v.asInstanceOf[AnyRef]
    case BSONLong(v) => v.asInstanceOf[AnyRef]
    case BSONDouble(v) => v.asInstanceOf[AnyRef]
    case BSONString(v) => v
    case x: BSONDocument => x
    case x: BSONArray => x
    case BSONTimestamp(v) => new java.util.Date(v)
  }

  //TODO
  def fallbackValueFromDbObject(dbo: BSONDocument, fieldNames: List[String]): Option[_] = {
    Option(fieldNames.foldLeft(dbo: AnyRef)((obj: AnyRef, fieldName: String) => {
      obj match {
        case dbl: BSONArray =>
          dbl.values.map(_.asInstanceOf[BSONDocument]).flatMap(_.get(fieldName)).collect(bsonValueToAnyRef).toList
        case dbo: BSONDocument =>
          dbo.get(fieldName).collectFirst[AnyRef](bsonValueToAnyRef).orNull
        case null => null
      }
    }))
  }
}
