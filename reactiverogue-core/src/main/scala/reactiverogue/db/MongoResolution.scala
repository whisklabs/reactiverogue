package reactiverogue.db

import reactivemongo.api.{DefaultDB, MongoConnection}

import scala.concurrent.{ExecutionContext, Future}

case class MongoResolution(conn: MongoConnection, dbName: String) {

  def database(implicit ec: ExecutionContext): Future[DefaultDB] = {
    conn.database(dbName)
  }
}
