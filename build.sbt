import Dependencies._
import common._

lazy val mongodb =
  module("reactiverogue-mongodb")
    .settings(
      libraryDependencies ++= mongoDependencies)

lazy val record =
  module("reactiverogue-record")
    .dependsOn(mongodb)

lazy val core =
  module("reactiverogue-core")
    .dependsOn(mongodb)
    .settings(
      libraryDependencies ++= coreDependencies)
    
lazy val dsl =
  module("reactiverogue-record-dsl")
    .dependsOn(core, record)
    .settings(
      libraryDependencies ++= recordDslDependencies)