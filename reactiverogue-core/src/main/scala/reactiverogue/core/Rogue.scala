package reactiverogue.core

import java.util.Date
import org.joda.time.DateTime
import reactiverogue.bson.BSONSerializable
import scala.language.implicitConversions
import reactivemongo.bson._

/**
  * A utility trait containing typing shorthands, and a collection of implicit conversions that make query
  * syntax much simpler.
  *
  * @see AbstractQuery for an example of the use of implicit conversions.
  */
trait Rogue {

  // QueryField implicits
//  implicit def rbooleanFieldtoQueryField[M](f: Field[Boolean, M]): QueryField[Boolean, M] = new QueryField(f)
  //  implicit def rcharFieldtoQueryField[M](f: Field[Char, M]): QueryField[Char, M] = new QueryField(f)

  //  implicit def rbyteFieldtoNumericQueryField[M](f: Field[Byte, M]): NumericQueryField[Byte, M] = new NumericQueryField(f)
  //  implicit def rshortFieldtoNumericQueryField[M](f: Field[Short, M]): NumericQueryField[Short, M] = new NumericQueryField(f)
//  implicit def rintFieldtoNumericQueryField[M](f: Field[Int, M]): NumericQueryField[Int, M] = new NumericQueryField(f)
//  implicit def rlongFieldtoNumericQueryField[M](f: Field[Long, M]): NumericQueryField[Long, M] = new NumericQueryField(f)
  //  implicit def rjlongFieldtoNumericQueryField[F <: java.lang.Long, M](f: Field[F, M]): NumericQueryField[F, M] = new NumericQueryField(f)
  //  implicit def rfloatFieldtoNumericQueryField[M](f: Field[Float, M]): NumericQueryField[Float, M] = new NumericQueryField(f)
//  implicit def rdoubleFieldtoNumericQueryField[M](f: Field[Double, M]): NumericQueryField[Double, M] = new NumericQueryField(f)
//
//  implicit def rstringFieldToStringQueryField[F <: String, M](f: Field[F, M]): StringQueryField[F, M] = new StringQueryField(f)
//  implicit def rdateFieldToDateQueryField[M](f: Field[Date, M]): DateQueryField[M] = new DateQueryField(f)
//  implicit def rdbobjectFieldToQueryField[M](f: Field[BSONDocument, M]): QueryField[BSONDocument, M] = new QueryField(f)
//
//  implicit def rseqFieldToSeqQueryField[M, F: BSONSerializable](f: Field[Seq[F], M]): SeqQueryField[F, M] = new SeqQueryField[F, M](f)
//  implicit def rmapFieldToMapQueryField[M, F](f: Field[Map[String, F], M]): MapQueryField[F, M] = new MapQueryField[F, M](f)

  /**
    * ModifyField implicits
    *
    * These are dangerous in the general case, unless the field type can be safely serialized
    * or the field class handles necessary serialization. We specialize some safe cases.
    */
//  implicit def rfieldToSafeModifyField[M, F](f: Field[F, M]): SafeModifyField[F, M] = new SafeModifyField(f)
//  implicit def booleanRFieldToModifyField[M](f: Field[Boolean, M]): ModifyField[Boolean, M] = new ModifyField(f)
  //  implicit def charRFieldToModifyField[M](f: Field[Char, M]): ModifyField[Char, M] = new ModifyField(f)

  // SelectField implicits
  class Flattened[A, B]
  implicit def anyValIsFlattened[A <: AnyVal]: Flattened[A, A] = new Flattened[A, A]
  implicit def enumIsFlattened[A <: Enumeration#Value]: Flattened[A, A] = new Flattened[A, A]
  implicit val stringIsFlattened = new Flattened[String, String]
  implicit val objectIdIsFlattened = new Flattened[BSONObjectID, BSONObjectID]
  implicit val dateIsFlattened = new Flattened[java.util.Date, java.util.Date]
  implicit def recursiveFlattenList[A, B](implicit ev: Flattened[A, B]) = new Flattened[List[A], B]
  implicit def recursiveFlattenSeq[A, B](implicit ev: Flattened[A, B]) = new Flattened[Seq[A], B]
}

object Rogue extends Rogue
