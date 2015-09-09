import dependencies._
import common._

commonSettings

packagedArtifacts in file(".") := Map.empty

lazy val bson =
  module("reactiverogue-bson")
    .settings(
      libraryDependencies ++= bsonDependencies)

lazy val json =
  module("reactiverogue-json")
    .settings(
      libraryDependencies ++= jsonDependencies)

lazy val core =
  module("reactiverogue-core")
    .dependsOn(bson, json)
    .settings(
      libraryDependencies ++= coreDependencies)

lazy val recordDsl =
  module("reactiverogue-record-dsl")
    .dependsOn(core)
    .settings(
      libraryDependencies ++= recordDslDependencies)
