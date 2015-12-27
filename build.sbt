import dependencies._
import common._

lazy val root =
  project.in(file("."))
    .settings(commonSettings: _*)
    .settings(
      publish := {},
      publishLocal := {})
    .aggregate(bson, core, recordDsl)

lazy val bson =
  module("reactiverogue-bson")
    .settings(
      libraryDependencies ++= bsonDependencies)

lazy val core =
  module("reactiverogue-core")
    .dependsOn(bson)
    .settings(
      libraryDependencies ++= coreDependencies)

lazy val recordDsl =
  module("reactiverogue-record-dsl")
    .dependsOn(core)
    .settings(
      libraryDependencies ++= recordDslDependencies)
