import dependencies._
import common._

commonSettings

packagedArtifacts in file(".") := Map.empty

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

lazy val recordDsl =
  module("reactiverogue-record-dsl")
    .dependsOn(record)
    .settings(
      libraryDependencies ++= recordDslDependencies)
