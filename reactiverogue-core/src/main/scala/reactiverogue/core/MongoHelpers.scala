// Copyright 2011 Foursquare Labs Inc. All Rights Reserved.

package reactiverogue.core

import reactivemongo.bson._
import scala.collection.immutable.ListMap
import collection.mutable.ListBuffer
import play.modules.reactivemongo.json.BSONFormats
import play.api.libs.json.{ Json, JsObject }

object MongoHelpers extends Rogue {
  case class AndCondition(clauses: List[QueryClause[_]], orCondition: Option[OrCondition]) {
    def isEmpty: Boolean = clauses.isEmpty && orCondition.isEmpty
  }

  case class OrCondition(conditions: List[AndCondition])

  sealed case class MongoOrder(terms: List[(String, Boolean)])

  sealed case class MongoModify(clauses: List[ModifyClause])

  sealed case class MongoSelect[M, R](fields: List[SelectField[_, M]], transformer: List[Any] => R)

  object MongoBuilder {
    def buildCondition(cond: AndCondition, signature: Boolean = false): BSONDocument = {
      buildCondition(cond, ListBuffer.empty, signature)
    }

    def buildCondition(cond: AndCondition,
      buffer: ListBuffer[(String, BSONValue)],
      signature: Boolean): BSONDocument = {
      val (rawClauses, safeClauses) = cond.clauses.partition(_.isInstanceOf[RawQueryClause])

      // Normal clauses
      safeClauses.groupBy(_.fieldName).toList
        .sortBy { case (fieldName, _) => -cond.clauses.indexWhere(_.fieldName == fieldName) }
        .foreach {
          case (name, cs) => {
            // Equality clauses look like { a : 3 }
            // but all other clauses look like { a : { $op : 3 }}
            // and can be chained like { a : { $gt : 2, $lt: 6 }}.
            // So if there is any equality clause, apply it (only) to the builder;
            // otherwise, chain the clauses.
            cs.filter(i => i.isInstanceOf[EqClause[_, _]] || i.isInstanceOf[RegexQueryClause[_]]).headOption match {
              case Some(eqClause) => eqClause.extend(buffer, signature)
              case None => {
                val nameBuff = ListBuffer.empty[(String, BSONValue)]

                cs.foreach(_.extend(nameBuff, signature))
                buffer += name -> BSONDocument(nameBuff)
              }
            }
          }
        }

      // Raw clauses
      rawClauses.foreach(_.extend(buffer, signature))

      // Optional $or clause (only one per "and" chain)
      cond.orCondition.foreach(or => {
        val subclauses = or.conditions
          .map(buildCondition(_, signature))
          .filterNot(_.isEmpty)
        buffer += "$or" -> BSONArray(subclauses.map(v => util.Try(v)).toStream)
      })
      BSONDocument(buffer)
    }

    def buildOrder(o: MongoOrder): BSONDocument = {
      val seq = o.terms.reverse.map { case (field, ascending) => field -> BSONInteger(if (ascending) 1 else -1) }
      BSONDocument(seq)
    }

    def buildModify(m: MongoModify): BSONDocument = {
      val seq = m.clauses.groupBy(_.operator).map {
        case (op, cs) =>
          val nameBuff = ListBuffer.empty[(String, BSONValue)]
          cs.foreach(_.extend(nameBuff))
          op.toString() -> BSONDocument(nameBuff)
      }
      BSONDocument(seq)
    }

    //    def jsonString(doc: BSONDocument): String = {
    //      compact(render(JObjectParser.serialize(doc)(DefaultFormats)))
    //    }

    implicit class BSONDocumentIsJsonString(doc: BSONDocument) {

      def str = Json.stringify(BSONFormats.BSONDocumentFormat.writes(doc))
    }

    def buildSelect[M, R](select: MongoSelect[M, R]): BSONDocument = {
      // If select.fields is empty, then a MongoSelect clause exists, but has an empty
      // list of fields. In this case (used for .exists()), we select just the
      // _id field.
      if (select.fields.isEmpty) {
        BSONDocument("_id" -> BSONInteger(1))
      } else {
        val seq: List[(String, BSONValue)] = select.fields.map { f =>
          f.field.name -> (f.slc match {
            case None => BSONInteger(1)
            case Some((s, None)) => BSONDocument("$slice" -> BSONInteger(s))
            case Some((s, Some(e))) => BSONDocument("$slice" -> BSONArray(s, e))
          })
        }
        BSONDocument(seq)
      }
    }

    def buildHint(h: ListMap[String, BSONValue]): BSONDocument = {
      BSONDocument(h)
    }

    def buildQueryString[R, M](operation: String, collectionName: String, query: Query[M, R, _]): String = {
      val sb = new StringBuilder("db.%s.%s(".format(collectionName, operation))
      sb.append(buildCondition(query.condition, signature = false).str)
      query.select.foreach(s => sb.append("," + buildSelect(s).str))
      sb.append(")")
      query.order.foreach(o => sb.append(".sort(%s)" format buildOrder(o).str))
      query.lim.foreach(l => sb.append(".limit(%d)" format l))
      query.sk.foreach(s => sb.append(".skip(%d)" format s))
      query.maxScan.foreach(m => sb.append("._addSpecial(\"$maxScan\", %d)" format m))
      query.comment.foreach(c => sb.append("._addSpecial(\"$comment\", \"%s\")" format c))
      query.hint.foreach(h => sb.append(".hint(%s)" format buildHint(h).str))
      sb.toString
    }

    def buildConditionString[R, M](operation: String, collectionName: String, query: Query[M, R, _]): String = {
      val sb = new StringBuilder("db.%s.%s(".format(collectionName, operation))
      sb.append(buildCondition(query.condition, signature = false).str)
      sb.append(")")
      sb.toString
    }

    def buildModifyString[R, M](collectionName: String, modify: ModifyQuery[M, _],
      upsert: Boolean = false, multi: Boolean = false): String = {
      "db.%s.update(%s,%s,%s,%s)".format(
        collectionName,
        buildCondition(modify.query.condition, signature = false).str,
        buildModify(modify.mod).str,
        upsert,
        multi
      )
    }

    def buildFindAndModifyString[R, M](collectionName: String, mod: FindAndModifyQuery[M, R], returnNew: Boolean, upsert: Boolean, remove: Boolean): String = {
      val query = mod.query
      val sb = new StringBuilder("db.%s.findAndModify({query:%s".format(
        collectionName, buildCondition(query.condition).str))
      query.order.foreach(o => sb.append(",sort:" + buildOrder(o).str))
      if (remove) sb.append(",remove:true")
      sb.append(",update:" + buildModify(mod.mod).str)
      sb.append(",new:" + returnNew)
      query.select.foreach(s => sb.append(",fields:" + buildSelect(s).str))
      sb.append(",upsert:" + upsert)
      sb.append("})")
      sb.toString
    }

    def buildSignature[R, M](collectionName: String, query: Query[M, R, _]): String = {
      val sb = new StringBuilder("db.%s.find(".format(collectionName))
      sb.append(buildCondition(query.condition, signature = true).str)
      sb.append(")")
      query.order.foreach(o => sb.append(".sort(%s)" format buildOrder(o).str))
      sb.toString
    }
  }
}
