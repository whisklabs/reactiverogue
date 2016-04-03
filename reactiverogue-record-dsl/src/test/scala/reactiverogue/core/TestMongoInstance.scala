package reactiverogue.core

import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest.Suite
import reactivemongo.api.{ DefaultDB, MongoConnection, MongoDriver }

trait TestMongoInstance extends DockerTestKit with DockerMongodbService { self: Suite =>

  def mongodbPort: Int = mongodbContainer.getPorts().futureValue.apply(DefaultMongodbPort)
  def mongodbHost: String = docker.host

  def mongoUri = s"mongodb://$mongodbHost:$mongodbPort/test"

  protected implicit var mongodb: DefaultDB = _

  override def beforeAll() = {
    super.beforeAll()
    val driver = new MongoDriver
    val uri = MongoConnection.parseURI(mongoUri).get
    mongodb = driver.connection(MongoConnection.parseURI(mongoUri).get)(uri.db.getOrElse("test"))
  }

  override def afterAll() = {
    mongodb.connection.close()
  }
}

