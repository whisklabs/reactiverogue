val ReactivemongoVer = "0.20.13"

val playVer = "2.6.9"

lazy val commonSettings = Seq(
  organization := "com.whisk",
  scalaVersion := "2.12.6",
  version := "0.6.0",
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  publishMavenStyle := true,
  credentials += Credentials(Path.userHome / ".m2" / ".credentials"),
  publishTo := Some("internal.repo" at "https://mymavenrepo.com/repo/y2J1NBZ7K79ReLGgRL3s/"),
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
    .settings(
      libraryDependencies ++= Seq("org.reactivemongo" %% "reactivemongo-bson" % ReactivemongoVer))

lazy val core =
  module("reactiverogue-core")
    .dependsOn(bson)
    .settings(libraryDependencies ++= Seq(
      "org.reactivemongo" %% "reactivemongo" % ReactivemongoVer,
      "org.reactivemongo" %% "reactivemongo-iteratees" % ReactivemongoVer,
      "org.reactivemongo" %% "reactivemongo-play-json" % "0.20.13-play26",
      "com.typesafe.play" %% "play-json" % playVer
    ))

lazy val recordDsl =
  module("reactiverogue-record-dsl")
    .dependsOn(core)
    .settings(libraryDependencies ++= Seq(
      "junit" % "junit" % "4.12" % "test",
      "org.scalatest" %% "scalatest" % "3.0.1" % "test",
      "com.whisk" %% "docker-testkit-scalatest" % "0.9.1" % "test",
      "com.whisk" %% "docker-testkit-impl-spotify" % "0.9.1" % "test"
    ))
