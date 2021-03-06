package reactiverogue.core

import java.util.Date

import reactivemongo.bson._
import reactiverogue.bson.BSONSerializable
import reactiverogue.core.MongoHelpers.AndCondition
import reactiverogue.record._
import reactiverogue.record.field._

import scala.language.implicitConversions

trait QueryImplicits extends Rogue {
  def OrQuery[M <: MongoRecord[M], R](subqueries: Query[M, R, _]*)
    : Query[M, R, Unordered with Unselected with Unlimited with Unskipped with HasOrClause] = {
    subqueries.toList match {
      case Nil => throw new RogueException("No subqueries supplied to OrQuery", null)
      case q :: qs =>
        val orCondition = QueryHelpers.orConditionFromQueries(q :: qs)
        Query[M, R, Unordered with Unselected with Unlimited with Unskipped with HasOrClause](
          q.meta,
          q.collectionName,
          None,
          None,
          None,
          None,
          None,
          AndCondition(Nil, Some(orCondition)),
          None,
          None,
          None)
    }
  }

  /* Following are a collection of implicit conversions which take a meta-record and convert it to
   * a QueryBuilder. This allows users to write queries as "QueryType where ...".
   */
  implicit def metaRecordToQueryBuilder[M <: MongoRecord[M]](
      rec: M with MongoMetaRecord[M]): Query[M, M, InitialState] =
    Query[M, M, InitialState](rec,
                              rec.collectionName,
                              None,
                              None,
                              None,
                              None,
                              None,
                              AndCondition(Nil, None),
                              None,
                              None,
                              None)

  implicit def queryToLiftQuery[M <: MongoRecord[_], R, State](query: Query[M, R, State])(
      implicit ev: ShardingOk[M with MongoMetaRecord[_], State]): ExecutableQuery[
    MongoRecord[_] with MongoMetaRecord[_],
    M with MongoMetaRecord[_],
    R,
    State] = {
    ExecutableQuery(
      query.asInstanceOf[Query[M with MongoMetaRecord[_], R, State]],
      LiftQueryExecutor
    )
  }

  implicit def modifyQueryToLiftModifyQuery[M <: MongoRecord[_], State](
      query: ModifyQuery[M, State]): ExecutableModifyQuery[MongoRecord[_] with MongoMetaRecord[_],
                                                           M with MongoMetaRecord[_],
                                                           State] = {
    ExecutableModifyQuery(
      query.asInstanceOf[ModifyQuery[M with MongoMetaRecord[_], State]],
      LiftQueryExecutor
    )
  }

  implicit def findAndModifyQueryToLiftFindAndModifyQuery[M <: MongoRecord[_], R](
      query: FindAndModifyQuery[M, R])
    : ExecutableFindAndModifyQuery[MongoRecord[_] with MongoMetaRecord[_],
                                   M with MongoMetaRecord[_],
                                   R] = {
    ExecutableFindAndModifyQuery(
      query.asInstanceOf[FindAndModifyQuery[M with MongoMetaRecord[_], R]],
      LiftQueryExecutor
    )
  }

  implicit def metaRecordToLiftQuery[M <: MongoRecord[M]](
      rec: M with MongoMetaRecord[M]): ExecutableQuery[MongoRecord[_] with MongoMetaRecord[_],
                                                       M with MongoMetaRecord[_],
                                                       M,
                                                       InitialState] = {
    val queryBuilder = metaRecordToQueryBuilder(rec)
    val liftQuery = queryToLiftQuery(queryBuilder)
    liftQuery
  }

  implicit def fieldToQueryField[M <: BsonRecord[M], F: BSONSerializable](
      f: Field[F, M]): QueryField[F, M] = new QueryField(f)

  implicit def optionalFieldToQueryField[M <: BsonRecord[M], F: BSONSerializable](
      f: OptionalRecordField[F, M]): OptionalQueryField[F, M] = new OptionalQueryField(f)

  implicit def bsonRecordFieldToBsonRecordQueryField[M <: BsonRecord[M], B <: BsonRecord[B]](
      f: BsonRecordField[M, B]): BsonRecordQueryField[M, B] = {
    val rec = f.defaultValue // a hack to get at the embedded record
    new BsonRecordQueryField[M, B](f, _.asBSONDocument, rec)
  }

  implicit def rbsonRecordFieldToBsonRecordQueryField[M <: BsonRecord[M], B <: BsonRecord[B]](
      f: Field[B, M]): BsonRecordQueryField[M, B] = {
    // a hack to get at the embedded record
    val owner = f.owner
    if (f.name.indexOf('.') >= 0) {
      val fieldName = f.name.takeWhile(_ != '.')
      val field = owner
        .fieldByName(fieldName)
        .getOrElse(throw new Exception("Error getting field " + fieldName + " for " + owner))
      val typedField = field.asInstanceOf[BsonRecordListField[M, B]]
      // a gross hack to get at the embedded record
      val rec: B = typedField.zero
      new BsonRecordQueryField[M, B](f, _.asBSONDocument, rec)
    } else {
      val fieldName = f.name
      val field = owner
        .fieldByName(fieldName)
        .getOrElse(throw new Exception("Error getting field " + fieldName + " for " + owner))
      val typedField = field.asInstanceOf[BsonRecordField[M, B]]
      val rec: B = typedField.defaultValue
      new BsonRecordQueryField[M, B](f, _.asBSONDocument, rec)
    }
  }

  implicit def bsonRecordListFieldToBsonRecordListQueryField[M <: BsonRecord[M],
                                                             B <: BsonRecord[B]](
      f: BsonRecordListField[M, B]): BsonRecordListQueryField[M, B] = {
    new BsonRecordListQueryField[M, B](f, f.zero, _.asBSONDocument)
  }

  implicit def dateFieldToDateQueryField[M <: BsonRecord[M]](
      f: Field[java.util.Date, M]): DateQueryField[M] =
    new DateQueryField(f)

  implicit def ccFieldToQueryField[M <: BsonRecord[M], F](
      f: JsObjectField[M, F]): JsonTypeQueryField[F, M] =
    new JsonTypeQueryField[F, M](f)

  implicit def jsObjectListFieldToListQueryField[M <: BsonRecord[M], F](
      f: JsObjectListField[M, F]): JsonTypeListQueryField[F, M] =
    new JsonTypeListQueryField[F, M](f, f.valueToBsonValue)

  implicit def doubleFieldtoNumericQueryField[M <: BsonRecord[M], F](
      f: Field[Double, M]): NumericQueryField[Double, M] =
    new NumericQueryField(f)

  implicit def enumFieldToEnumNameQueryField[M <: BsonRecord[M], F <: Enumeration#Value](
      f: Field[F, M]): EnumNameQueryField[M, F] =
    new EnumNameQueryField(f)

  implicit def enumFieldToEnumQueryField[M <: BsonRecord[M], F <: Enumeration](
      f: EnumField[M, F]): EnumIdQueryField[M, F#Value] =
    new EnumIdQueryField(f)

  implicit def enumerationListFieldToEnumerationListQueryField[M <: BsonRecord[M],
                                                               F <: Enumeration#Value](
      f: Field[List[F], M]): EnumerationListQueryField[F, M] =
    new EnumerationListQueryField[F, M](f)

  implicit def intFieldtoNumericQueryField[M <: BsonRecord[M], F](
      f: Field[Int, M]): NumericQueryField[Int, M] =
    new NumericQueryField(f)

  implicit def latLongFieldToGeoQueryField[M <: BsonRecord[M]](
      f: Field[LatLong, M]): GeoQueryField[M] =
    new GeoQueryField(f)

  implicit def listFieldToListQueryField[M <: BsonRecord[M], F: BSONSerializable](
      f: Field[List[F], M]): ListQueryField[F, M] =
    new ListQueryField[F, M](f)

  implicit def stringsListFieldToStringsListQueryField[M <: BsonRecord[M]](
      f: Field[List[String], M]): StringsListQueryField[M] =
    new StringsListQueryField[M](f)

  implicit def longFieldtoNumericQueryField[M <: BsonRecord[M]](
      f: Field[Long, M]): NumericQueryField[Long, M] =
    new NumericQueryField(f)

  implicit def objectIdFieldToObjectIdQueryField[M <: BsonRecord[M]](
      f: Field[BSONObjectID, M]): ObjectIdQueryField[M] =
    new ObjectIdQueryField(f)

  implicit def mapFieldToMapQueryField[M <: BsonRecord[M], F](
      f: Field[Map[String, F], M]): MapQueryField[F, M] =
    new MapQueryField[F, M](f)

  implicit def stringFieldToStringQueryField[F <: String, M <: BsonRecord[M]](
      f: Field[F, M]): StringQueryField[F, M] =
    new StringQueryField(f)

  // ModifyField implicits
  implicit def fieldToModifyField[M <: BsonRecord[M], F: BSONSerializable](
      f: Field[F, M]): ModifyField[F, M] = new ModifyField(f)
  implicit def fieldToSafeModifyField[M <: BsonRecord[M], F](
      f: Field[F, M]): SafeModifyField[F, M] = new SafeModifyField(f)

  implicit def bsonRecordFieldToBsonRecordModifyField[M <: BsonRecord[M], B <: BsonRecord[B]](
      f: BsonRecordField[M, B]): BsonRecordModifyField[M, B] =
    new BsonRecordModifyField[M, B](f, _.asBSONDocument)

  implicit def bsonRecordListFieldToBsonRecordListModifyField[M <: BsonRecord[M],
                                                              B <: BsonRecord[B]](
      f: BsonRecordListField[M, B])(implicit mf: Manifest[B]): BsonRecordListModifyField[M, B] = {
    new BsonRecordListModifyField[M, B](f, f.zero, _.asBSONDocument)(mf)
  }

  implicit def dateFieldToDateModifyField[M <: BsonRecord[M]](
      f: Field[Date, M]): DateModifyField[M] =
    new DateModifyField(f)

  implicit def jsObjectFieldToModifyField[M <: BsonRecord[M], V](
      f: JsObjectField[M, V]): JsonTypeModifyField[V, M] =
    new JsonTypeModifyField[V, M](f, f.valueToBsonValue)

  implicit def jsObjectListFieldToListModifyField[M <: BsonRecord[M], V](
      f: JsObjectListField[M, V]): JsonTypeListModifyField[V, M] =
    new JsonTypeListModifyField[V, M](f, f.valueToBsonValue)

  implicit def doubleFieldToNumericModifyField[M <: BsonRecord[M]](
      f: Field[Double, M]): NumericModifyField[Double, M] =
    new NumericModifyField(f)

  implicit def enumerationFieldToEnumerationModifyField[M <: BsonRecord[M],
                                                        F <: Enumeration#Value](
      f: Field[F, M]): EnumerationModifyField[M, F] =
    new EnumerationModifyField(f)

  implicit def enumerationListFieldToEnumerationListModifyField[M <: BsonRecord[M],
                                                                F <: Enumeration#Value](
      f: Field[List[F], M]): EnumerationListModifyField[F, M] =
    new EnumerationListModifyField[F, M](f)

  implicit def intFieldToIntModifyField[M <: BsonRecord[M]](
      f: Field[Int, M]): NumericModifyField[Int, M] =
    new NumericModifyField(f)

  implicit def latLongFieldToGeoQueryModifyField[M <: BsonRecord[M]](
      f: Field[LatLong, M]): GeoModifyField[M] =
    new GeoModifyField(f)

  implicit def listFieldToListModifyField[M <: BsonRecord[M], F: BSONSerializable](
      f: Field[List[F], M]): ListModifyField[F, M] =
    new ListModifyField[F, M](f)

  implicit def longFieldToNumericModifyField[M <: BsonRecord[M]](
      f: Field[Long, M]): NumericModifyField[Long, M] =
    new NumericModifyField(f)

  implicit def mapFieldToMapModifyField[M <: BsonRecord[M], F: BSONSerializable](
      f: Field[Map[String, F], M]): MapModifyField[F, M] =
    new MapModifyField[F, M](f)

  // SelectField implicits
  implicit def mandatoryFieldToSelectField[M <: BsonRecord[M], V](
      f: RequiredRecordField[V, M]): SelectField[V, M] =
    new MandatorySelectField(f)

  implicit def optionalFieldToSelectField[M <: BsonRecord[M], V](
      f: OptionalRecordField[V, M]): SelectField[Option[V], M] =
    new OptionalSelectField(f)

  implicit def roptionalFieldToSelectField[M, V](
      f: OptionalField[V, M]): SelectField[Option[V], M] = new OptionalSelectField(f)

  implicit def rrequiredFieldToSelectField[M, V](f: RequiredField[V, M]): SelectField[V, M] =
    new MandatorySelectField(f)

  class BsonRecordIsBSONSerializable[T <: BsonRecord[T]] extends BSONSerializable[T] {
    override def asBSONValue(v: T): BSONValue = v.asBSONDocument

    override def fromBSONValue = PartialFunction.empty
  }

  object _BsonRecordIsBSONType extends BsonRecordIsBSONSerializable[Nothing]

  implicit def BsonRecordIsBSONSerializable[T <: BsonRecord[T]]: BsonRecordIsBSONSerializable[T] =
    _BsonRecordIsBSONType.asInstanceOf[BsonRecordIsBSONSerializable[T]]

}

object QueryImplicits extends QueryImplicits
