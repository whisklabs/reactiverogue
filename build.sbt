import dependencies._
import common._

commonSettings

lazy val mongodb =
  module("reactiverogue-mongodb")
    .settings(
      libraryDependencies ++= mongoDependencies)

lazy val core =
  module("reactiverogue-core")
    .dependsOn(mongodb)
    .settings(
      libraryDependencies ++= coreDependencies)

lazy val record =
  module("reactiverogue-record")
    .dependsOn(core)
    .settings(
      libraryDependencies ++= recordDependencies)
    
lazy val dsl =
  module("reactiverogue-record-dsl")
    .dependsOn(record)
    .settings(
      libraryDependencies ++= recordDslDependencies)
