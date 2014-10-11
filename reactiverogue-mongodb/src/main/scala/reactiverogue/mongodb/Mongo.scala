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

import java.util.concurrent.ConcurrentHashMap
import reactivemongo.api._
import reactivemongo.api.collections.default.BSONCollection
import concurrent.ExecutionContext.Implicits.global
import concurrent.duration.FiniteDuration
import reactivemongo.core.nodeset.Authenticate
import scala.util.{ Failure, Success }

/*
* A trait for identfying Mongo instances
*/
trait MongoIdentifier {
  def jndiName: String
  override def toString() = "MongoIdentifier(" + jndiName + ")"
  override def hashCode() = jndiName.hashCode()
  override def equals(other: Any): Boolean = other match {
    case mi: MongoIdentifier => mi.jndiName == this.jndiName
    case _ => false
  }
}

/*
* The default MongoIdentifier
*/
case object DefaultMongoIdentifier extends MongoIdentifier {
  val jndiName = "test"
}

/*
* Wrapper for getting a reference to a db from the given Mongo instance
*/
case class MongoAddress(host: MongoHostBase, name: String) {
  def db: DefaultDB = host.connection(name)
}

object MongoAddress {

  def apply(uri: String): MongoAddress = {
    MongoConnection.parseURI(uri) match {
      case Success(MongoConnection.ParsedURI(hosts, opts, _, Some(db), auth)) =>
        MongoAddress(MongoHost(hosts.map(h => h._1 + ":" + h._2), opts, auth.toList), db)
      case Success(MongoConnection.ParsedURI(_, _, _, None, _)) =>
        throw new Exception(s"Missing database name in mongodb.uri '$uri'")
      case Failure(e) =>
        throw new Exception(s"Invalid mongodb.uri '$uri'")
    }
  }
}

/*
* Wrapper for creating a MongoConnection instance
*/
abstract class MongoHostBase {
  def connection: MongoConnection
}

case class MongoHost(nodes: Seq[String], options: MongoConnectionOptions, authentications: Seq[Authenticate] = Seq.empty) extends MongoHostBase {
  val driver = new MongoDriver
  lazy val connection = driver.connection(nodes, options, authentications)
}

object MongoHost {
  def apply(host: String): MongoHost = MongoHost(Seq(host), MongoConnectionOptions())
}

/*
* Main Mongo object
*/
object MongoDB {

  /*
  * HashMap of MongoAddresses, keyed by MongoIdentifier
  */
  private val dbs = new ConcurrentHashMap[MongoIdentifier, MongoAddress]

  /*
  * Define a Mongo db
  */
  def defineDb(name: MongoIdentifier, address: MongoAddress): Unit = {
    dbs.put(name, address)
  }

  /*
  * Define and authenticate a Mongo db
  */
  def defineDbAuth(name: MongoIdentifier, address: MongoAddress, username: String, password: String)(implicit timeout: FiniteDuration): Unit = {
    address.db.authenticate(username, password)
    dbs.put(name, address)
  }

  /*
  * Get a DB reference
  */
  def getDb(name: MongoIdentifier): Option[DefaultDB] = dbs.get(name) match {
    case null => None
    case ma: MongoAddress => Some(ma.db)
  }

  /*
  * Get a Mongo collection. Gets a Mongo db first.
  */
  private def getCollection(name: MongoIdentifier, collectionName: String): Option[BSONCollection] = {
    getDb(name).map(_.apply(collectionName))
  }

  /**
   * Executes function {@code f} with the mongo db named {@code name}.
   */
  def use[T](name: MongoIdentifier)(f: (DefaultDB) => T): T = {

    val db = getDb(name).getOrElse(throw new Exception("db not found, identifier: " + name))

    f(db)
  }

  /**
   * Executes function {@code f} with the mongo named {@code name}. Uses the default mongoIdentifier
   */
  def use[T](f: (DefaultDB) => T): T =
    use(DefaultMongoIdentifier)(f)

  /**
   * Executes function {@code f} with the mongo named {@code name} and collection names {@code collectionName}.
   * Gets a collection for you.
   */
  def useCollection[T](name: MongoIdentifier, collectionName: String)(f: (BSONCollection) => T): T = {
    val coll = getCollection(name, collectionName) match {
      case Some(collection) => collection
      case _ => throw new Exception("BSONCollection not found: " + collectionName + ". MongoIdentifier: " + name.toString)
    }

    f(coll)
  }

  /**
   * Same as above except uses DefaultMongoIdentifier
   */
  def useCollection[T](collectionName: String)(f: (BSONCollection) => T): T =
    useCollection(DefaultMongoIdentifier, collectionName)(f)

  /**
   * Executes function {@code f} with the mongo db named {@code name}.
   */
  def useSession[T](name: MongoIdentifier)(f: (DefaultDB) => T): T = {

    val db = getDb(name) match {
      case Some(mongo) => mongo
      case _ => throw new Exception("Mongo not found: " + name.toString)
    }

    f(db)
  }

  /**
   * Same as above except uses DefaultMongoIdentifier
   */
  def useSession[T](f: (DefaultDB) => T): T =
    useSession(DefaultMongoIdentifier)(f)

  //
  def close: Unit = {
    dbs.clear
  }
}
