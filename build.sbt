import dependencies._

lazy val commonSettings = Seq(
  organization := "com.whisk",
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.11.8"),
  version := "0.5.0.rc1",
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  sonatypeProfileName := "com.whisk",
  pomExtra in Global := {
    <url>https://github.com/whisklabs/docker-it-scala</url>
      <scm>
        <connection>scm:git:github.com/whisklabs/reactiverogue.git</connection>
        <developerConnection>scm:git:git@github.com:whisklabs/reactiverogue.git</developerConnection>
        <url>github.com/whisklabs/reactiverogue.git</url>
      </scm>
      <developers>
        <developer>
          <id>viktortnk</id>
          <name>Viktor Taranenko</name>
          <url>https://github.com/viktortnk</url>
        </developer>
      </developers>
  }
)

def module(name: String) =
  Project(name, file(name))
    .settings(commonSettings: _*)
    .settings(
      scalacOptions ++= Seq("-feature", "-deprecation")
    )

lazy val root =
  project
    .in(file("."))
    .settings(commonSettings: _*)
    .settings(publish := {}, publishLocal := {}, packagedArtifacts := Map.empty)
    .aggregate(bson, core, recordDsl)

lazy val bson =
  module("reactiverogue-bson")
    .settings(libraryDependencies ++= bsonDependencies)

lazy val core =
  module("reactiverogue-core")
    .dependsOn(bson)
    .settings(libraryDependencies ++= coreDependencies)

lazy val recordDsl =
  module("reactiverogue-record-dsl")
    .dependsOn(core)
    .settings(libraryDependencies ++= recordDslDependencies)
