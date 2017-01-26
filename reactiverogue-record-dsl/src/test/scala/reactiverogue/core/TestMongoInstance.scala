package reactiverogue.core

import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest.Suite
import reactivemongo.api.{MongoConnection, MongoDriver}
import reactiverogue.db.MongoResolution

trait TestMongoInstance extends DockerTestKit with DockerMongodbService { self: Suite =>

  def mongodbPort: Int = mongodbContainer.getPorts().futureValue.apply(DefaultMongodbPort)
  def mongodbHost: String = dockerExecutor.host

  def mongoUri = s"mongodb://$mongodbHost:$mongodbPort/test"

  protected implicit var mongores: MongoResolution = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    val driver = new MongoDriver
    val uri = MongoConnection.parseURI(mongoUri).get
    mongores = MongoResolution(driver.connection(MongoConnection.parseURI(mongoUri).get),
                               uri.db.getOrElse("test"))
  }

  override def afterAll(): Unit = {
    mongores.conn.close()
  }
}
