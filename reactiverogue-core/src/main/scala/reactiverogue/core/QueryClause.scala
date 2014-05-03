// Copyright 2011 Foursquare Labs Inc. All Rights Reserved.

package reactiverogue.core

import java.util.regex.Pattern
import scala.collection.mutable.ListBuffer
import reactivemongo.bson._
import reactiverogue.mongodb.BSONSerializable

abstract class QueryClause[+V](val fieldName: String, val actualIndexBehavior: MaybeIndexed, val conditions: (CondOps.Value, BSONValue)*) {
  def extend(q: ListBuffer[(String, BSONValue)], signature: Boolean): Unit = {
    conditions foreach {
      case (op, v) =>
        q += op.toString -> (if (signature) BSONInteger(0) else v)
    }
  }
  val expectedIndexBehavior: MaybeIndexed = Index
  def withExpectedIndexBehavior(b: MaybeIndexed): QueryClause[V]
}

abstract class IndexableQueryClause[V, Ind <: MaybeIndexed](fname: String, actualIB: Ind, conds: (CondOps.Value, BSONValue)*)
  extends QueryClause[V](fname, actualIB, conds: _*)

trait ShardKeyClause

case class AllQueryClause[V](override val fieldName: String, vs: List[BSONValue], override val expectedIndexBehavior: MaybeIndexed = Index)
    extends IndexableQueryClause[List[V], Index](fieldName, Index, CondOps.All -> BSONArray(vs)) {
  override def withExpectedIndexBehavior(b: MaybeIndexed): AllQueryClause[V] = this.copy(expectedIndexBehavior = b)
}

case class InQueryClause[V](override val fieldName: String, vs: List[BSONValue], override val expectedIndexBehavior: MaybeIndexed = Index)
    extends IndexableQueryClause[List[V], Index](fieldName, Index, CondOps.In -> BSONArray(vs)) {
  override def withExpectedIndexBehavior(b: MaybeIndexed): InQueryClause[V] = this.copy(expectedIndexBehavior = b)
}

case class GtQueryClause[V](override val fieldName: String, v: BSONValue, override val expectedIndexBehavior: MaybeIndexed = Index)
    extends IndexableQueryClause[V, PartialIndexScan](fieldName, PartialIndexScan, CondOps.Gt -> v) {
  override def withExpectedIndexBehavior(b: MaybeIndexed): GtQueryClause[V] = this.copy(expectedIndexBehavior = b)
}

case class GtEqQueryClause[V](override val fieldName: String, v: BSONValue, override val expectedIndexBehavior: MaybeIndexed = Index)
    extends IndexableQueryClause[V, PartialIndexScan](fieldName, PartialIndexScan, CondOps.GtEq -> v) {
  override def withExpectedIndexBehavior(b: MaybeIndexed): GtEqQueryClause[V] = this.copy(expectedIndexBehavior = b)
}

case class LtQueryClause[V](override val fieldName: String, v: BSONValue, override val expectedIndexBehavior: MaybeIndexed = Index)
    extends IndexableQueryClause[V, PartialIndexScan](fieldName, PartialIndexScan, CondOps.Lt -> v) {
  override def withExpectedIndexBehavior(b: MaybeIndexed): LtQueryClause[V] = this.copy(expectedIndexBehavior = b)
}

case class LtEqQueryClause[V](override val fieldName: String, v: BSONValue, override val expectedIndexBehavior: MaybeIndexed = Index)
    extends IndexableQueryClause[V, PartialIndexScan](fieldName, PartialIndexScan, CondOps.LtEq -> v) {
  override def withExpectedIndexBehavior(b: MaybeIndexed): LtEqQueryClause[V] = this.copy(expectedIndexBehavior = b)
}

case class BetweenQueryClause[V](override val fieldName: String, lower: BSONValue, upper: BSONValue, override val expectedIndexBehavior: MaybeIndexed = Index)
    extends IndexableQueryClause[V, PartialIndexScan](fieldName, PartialIndexScan, CondOps.GtEq -> lower, CondOps.LtEq -> upper) {
  override def withExpectedIndexBehavior(b: MaybeIndexed): BetweenQueryClause[V] = this.copy(expectedIndexBehavior = b)
}

case class StrictBetweenQueryClause[V](override val fieldName: String, lower: BSONValue, upper: BSONValue, override val expectedIndexBehavior: MaybeIndexed = Index)
    extends IndexableQueryClause[V, PartialIndexScan](fieldName, PartialIndexScan, CondOps.Gt -> lower, CondOps.Lt -> upper) {
  override def withExpectedIndexBehavior(b: MaybeIndexed): StrictBetweenQueryClause[V] = this.copy(expectedIndexBehavior = b)
}

case class NeQueryClause[V](override val fieldName: String, v: BSONValue, override val expectedIndexBehavior: MaybeIndexed = Index)
    extends IndexableQueryClause[V, PartialIndexScan](fieldName, PartialIndexScan, CondOps.Ne -> v) {
  override def withExpectedIndexBehavior(b: MaybeIndexed): NeQueryClause[V] = this.copy(expectedIndexBehavior = b)
}

case class NearQueryClause[V](override val fieldName: String, v: BSONValue, override val expectedIndexBehavior: MaybeIndexed = Index)
    extends IndexableQueryClause[V, PartialIndexScan](fieldName, PartialIndexScan, CondOps.Near -> v) {
  override def withExpectedIndexBehavior(b: MaybeIndexed): NearQueryClause[V] = this.copy(expectedIndexBehavior = b)
}

case class NearSphereQueryClause[V](override val fieldName: String, lat: Double, lng: Double, radians: Radians, override val expectedIndexBehavior: MaybeIndexed = Index)
    extends IndexableQueryClause[V, PartialIndexScan](fieldName, PartialIndexScan) {
  override def extend(q: ListBuffer[(String, BSONValue)], signature: Boolean) {
    q += CondOps.NearSphere.toString -> (if (signature) BSONInteger(0) else BSONArray(lat, lng))
    q += CondOps.MaxDistance.toString -> (if (signature) BSONInteger(0) else BSONDouble(radians.value))
  }
  override def withExpectedIndexBehavior(b: MaybeIndexed): NearSphereQueryClause[V] = this.copy(expectedIndexBehavior = b)
}

case class ModQueryClause[V](override val fieldName: String, v: List[BSONValue], override val expectedIndexBehavior: MaybeIndexed = Index)
    extends IndexableQueryClause[List[V], IndexScan](fieldName, IndexScan, CondOps.Mod -> BSONArray(v)) {
  override def withExpectedIndexBehavior(b: MaybeIndexed): ModQueryClause[V] = this.copy(expectedIndexBehavior = b)
}

case class TypeQueryClause(override val fieldName: String, v: MongoType.Value, override val expectedIndexBehavior: MaybeIndexed = Index)
    extends IndexableQueryClause[Int, IndexScan](fieldName, IndexScan, CondOps.Type -> BSONInteger(v.id)) {
  override def withExpectedIndexBehavior(b: MaybeIndexed): TypeQueryClause = this.copy(expectedIndexBehavior = b)
}

case class ExistsQueryClause(override val fieldName: String, v: Boolean, override val expectedIndexBehavior: MaybeIndexed = Index)
    extends IndexableQueryClause[Boolean, IndexScan](fieldName, IndexScan, CondOps.Exists -> BSONBoolean(v)) {
  override def withExpectedIndexBehavior(b: MaybeIndexed): ExistsQueryClause = this.copy(expectedIndexBehavior = b)
}

case class NinQueryClause[V](override val fieldName: String, vs: List[BSONValue], override val expectedIndexBehavior: MaybeIndexed = Index)
    extends IndexableQueryClause[List[V], DocumentScan](fieldName, DocumentScan, CondOps.Nin -> BSONArray(vs)) {
  override def withExpectedIndexBehavior(b: MaybeIndexed): NinQueryClause[V] = this.copy(expectedIndexBehavior = b)
}

case class SizeQueryClause(override val fieldName: String, v: Int, override val expectedIndexBehavior: MaybeIndexed = Index)
    extends IndexableQueryClause[Int, DocumentScan](fieldName, DocumentScan, CondOps.Size -> BSONInteger(v)) {
  override def withExpectedIndexBehavior(b: MaybeIndexed): SizeQueryClause = this.copy(expectedIndexBehavior = b)
}

case class RegexQueryClause[Ind <: MaybeIndexed](override val fieldName: String, actualIB: Ind, p: Pattern, override val expectedIndexBehavior: MaybeIndexed = Index)
    extends IndexableQueryClause[Pattern, Ind](fieldName, actualIB) {
  val flagMap = Map(
    Pattern.CANON_EQ -> "c",
    Pattern.CASE_INSENSITIVE -> "i",
    Pattern.COMMENTS -> "x",
    Pattern.DOTALL -> "s",
    Pattern.LITERAL -> "t",
    Pattern.MULTILINE -> "m",
    Pattern.UNICODE_CASE -> "u",
    Pattern.UNIX_LINES -> "d"
  )

  def flagsToString(flags: Int) = {
    (for {
      (mask, char) <- flagMap
      if (flags & mask) != 0
    } yield char).mkString
  }

  override def extend(q: ListBuffer[(String, BSONValue)], signature: Boolean) {
    q += fieldName ->
      (if (signature) BSONDocument("$regex" -> 0, "$options" -> 0)
      else BSONSerializable[Pattern].asBSONValue(p))
  }

  override def withExpectedIndexBehavior(b: MaybeIndexed): RegexQueryClause[Ind] = this.copy(expectedIndexBehavior = b)
}

case class RawQueryClause(f: ListBuffer[(String, BSONValue)] => Unit, override val expectedIndexBehavior: MaybeIndexed = DocumentScan) extends IndexableQueryClause("raw", DocumentScan) {
  override def extend(q: ListBuffer[(String, BSONValue)], signature: Boolean) {
    f(q)
  }
  override def withExpectedIndexBehavior(b: MaybeIndexed): RawQueryClause = this.copy(expectedIndexBehavior = b)
}

case class EmptyQueryClause[V](override val fieldName: String, override val expectedIndexBehavior: MaybeIndexed = Index)
    extends IndexableQueryClause[List[V], Index](fieldName, Index) {
  override def extend(q: ListBuffer[(String, BSONValue)], signature: Boolean) {}
  override def withExpectedIndexBehavior(b: MaybeIndexed): EmptyQueryClause[V] = this.copy(expectedIndexBehavior = b)
}

case class EqClause[V, Ind <: MaybeIndexed](override val fieldName: String, value: BSONValue, override val expectedIndexBehavior: MaybeIndexed = Index)
    extends IndexableQueryClause[V, Index](fieldName, Index) {
  override def extend(q: ListBuffer[(String, BSONValue)], signature: Boolean): Unit = {
    q += fieldName -> (if (signature) BSONInteger(0) else value)
  }
  override def withExpectedIndexBehavior(b: MaybeIndexed): EqClause[V, Ind] = this.copy(expectedIndexBehavior = b)
}

case class WithinCircleClause[V](override val fieldName: String, lat: Double, lng: Double, radius: Double, override val expectedIndexBehavior: MaybeIndexed = Index)
    extends IndexableQueryClause[V, PartialIndexScan](fieldName, PartialIndexScan) {
  override def extend(q: ListBuffer[(String, BSONValue)], signature: Boolean): Unit = {
    val value: BSONValue =
      if (signature) BSONInteger(0)
      else BSONArray(BSONArray(lat, lng), BSONDouble(radius))
    q += "$within" -> BSONDocument("$center" -> value)
  }
  override def withExpectedIndexBehavior(b: MaybeIndexed): WithinCircleClause[V] = this.copy(expectedIndexBehavior = b)
}

case class WithinBoxClause[V](override val fieldName: String, lat1: Double, lng1: Double, lat2: Double, lng2: Double, override val expectedIndexBehavior: MaybeIndexed = Index)
    extends IndexableQueryClause[V, PartialIndexScan](fieldName, PartialIndexScan) {
  override def extend(q: ListBuffer[(String, BSONValue)], signature: Boolean): Unit = {
    val value: BSONValue = if (signature) BSONInteger(0) else {
      BSONArray(BSONArray(lat1, lng1), BSONArray(lat2, lng2))
    }
    q += "$within" -> BSONDocument("$box" -> value)
  }
  override def withExpectedIndexBehavior(b: MaybeIndexed): WithinBoxClause[V] = this.copy(expectedIndexBehavior = b)
}

case class ElemMatchWithPredicateClause[V](override val fieldName: String, clauses: Seq[QueryClause[_]], override val expectedIndexBehavior: MaybeIndexed = Index)
    extends IndexableQueryClause[V, DocumentScan](fieldName, DocumentScan) {
  override def extend(q: ListBuffer[(String, BSONValue)], signature: Boolean): Unit = {
    import reactiverogue.core.MongoHelpers.AndCondition
    val nestedBuff = ListBuffer.empty[(String, BSONValue)]
    MongoHelpers.MongoBuilder.buildCondition(AndCondition(clauses.toList, None), nestedBuff, signature)
    q += "$elemMatch" -> BSONDocument(nestedBuff)
  }
  override def withExpectedIndexBehavior(b: MaybeIndexed): ElemMatchWithPredicateClause[V] = this.copy(expectedIndexBehavior = b)
}

class ModifyClause(val operator: ModOps.Value, fields: (String, BSONValue)*) {
  def extend(q: ListBuffer[(String, BSONValue)]): Unit = {
    fields foreach { case (name, value) => q += name -> value }
  }
}

class ModifyAddEachClause[T](fieldName: String, values: Traversable[BSONValue])
    extends ModifyClause(ModOps.AddToSet) {
  override def extend(q: ListBuffer[(String, BSONValue)]): Unit = {
    q += fieldName -> BSONDocument("$each" -> BSONArray(values))
  }
}

class ModifyBitAndClause(fieldName: String, value: Int) extends ModifyClause(ModOps.Bit) {
  override def extend(q: ListBuffer[(String, BSONValue)]): Unit = {
    q += fieldName -> BSONDocument("and" -> BSONInteger(value))
  }
}

class ModifyBitOrClause(fieldName: String, value: Int) extends ModifyClause(ModOps.Bit) {
  override def extend(q: ListBuffer[(String, BSONValue)]): Unit = {
    q += fieldName -> BSONDocument("or" -> BSONInteger(value))
  }
}

class ModifyPullWithPredicateClause[V](fieldName: String, clauses: Seq[QueryClause[_]])
    extends ModifyClause(ModOps.Pull) {
  override def extend(q: ListBuffer[(String, BSONValue)]): Unit = {
    import reactiverogue.core.MongoHelpers.AndCondition
    MongoHelpers.MongoBuilder.buildCondition(AndCondition(clauses.toList, None), q, signature = false)
  }
}

class ModifyPullObjWithPredicateClause[V](fieldName: String, clauses: Seq[QueryClause[_]])
    extends ModifyClause(ModOps.Pull) {
  override def extend(q: ListBuffer[(String, BSONValue)]): Unit = {
    import reactiverogue.core.MongoHelpers.AndCondition
    val nestedBuff = ListBuffer.empty[(String, BSONValue)]
    MongoHelpers.MongoBuilder.buildCondition(AndCondition(clauses.toList, None), nestedBuff, signature = false)
    q += fieldName -> BSONDocument(nestedBuff)
  }
}
