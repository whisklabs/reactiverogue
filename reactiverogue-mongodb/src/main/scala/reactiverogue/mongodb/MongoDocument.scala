///*
// * Copyright 2010-2011 WorldWide Conferencing, LLC
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package reactiverogue.mongodb
//
//import reactivemongo.bson._
//import reactivemongo.api._
//import reactivemongo.core.commands.LastError
//import scala.concurrent.{ Future, ExecutionContext }
//
///*
//* extend case class with this trait
//*/
//trait MongoDocument[BaseDocument] extends JsonObject[BaseDocument] {
//  self: BaseDocument =>
//
//  def _id: Any
//
//  def meta: MongoDocumentMeta[BaseDocument]
//
//  //  def delete {
//  //    meta.delete("_id", _id)
//  //  }
//
//  def save(implicit executor: ExecutionContext) = meta.save(this)
//
//  def getRef: Option[MongoRef] = _id match {
//    case oid: BSONObjectID => Some(MongoRef(meta.collectionName, oid))
//    case _ => None
//  }
//}
//
///*
//* extend case class companion objects with this trait
//*/
//trait MongoDocumentMeta[BaseDocument] extends JsonObjectMeta[BaseDocument] with MongoMeta[BaseDocument] {
//
//  def create(dbo: BSONDocument): BaseDocument = {
//    create(JObjectParser.serialize(dbo).asInstanceOf[JObject])
//  }
//
//  /**
//   * Find a single row by a qry, using a BSONDocument.
//   */
//  def find(qry: BSONDocument)(implicit executor: ExecutionContext): Future[Option[BaseDocument]] = {
//    MongoDB.useCollection(mongoIdentifier, collectionName)(coll =>
//      coll.find(qry).one.map(opt => opt.map(create))
//    )
//  }
//
//  /**
//   * Find a single document by _id using an BSONObjectID.
//   */
//  def find(oid: BSONObjectID)(implicit executor: ExecutionContext): Future[Option[BaseDocument]] =
//    find(BSONDocument("_id" -> oid))
//
//
//  /**
//   * Find all documents in this collection
//   */
//  def findAll(implicit executor: ExecutionContext): Future[List[BaseDocument]] =
//    findAll(BSONDocument())
//
//  /**
//   * Find all documents using a BSONDocument query.
//   */
//  def findAll(qry: BSONDocument)(implicit executor: ExecutionContext): Future[List[BaseDocument]] =
//    MongoDB.useCollection(mongoIdentifier, collectionName)(coll =>
//      coll.find(qry).cursor[BSONDocument].toList.map(list => list.map(create))
//    )
//
//  /*
//  * Save a document to the db
//  */
//  def save(in: BaseDocument)(implicit executor: ExecutionContext): Future[LastError] = {
//    MongoDB.useCollection(mongoIdentifier, collectionName)(coll =>
//      coll.save(JObjectParser.parse(toJObject(in)))
//    )
//  }
//
//}
//
