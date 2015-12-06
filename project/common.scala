import bintray.BintrayPlugin.autoImport._
import sbt._
import Keys._
import com.typesafe.sbt.SbtScalariform.scalariformSettings

object common {

  val gitHeadCommitSha = settingKey[String]("current git commit SHA")

  val commonSettings = Seq(
    organization := "com.whisk",
    scalaVersion := "2.11.7",
    crossScalaVersions := Seq("2.11.7", "2.10.5"),
    gitHeadCommitSha := Process("git rev-parse --short HEAD").lines.head,
    version := "0.4.0.beta5",
    bintrayOrganization := Some("whisk"),
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    bintrayRepository := {
      if (version.value.trim.endsWith(gitHeadCommitSha.value)) "maven-snapshots" else "maven"
    })

  def module(name: String) =
    Project(name, file(name))
    .settings(commonSettings:_*)
    .settings(scalariformSettings:_*)
    .settings(
      resolvers += "Whisk" at "https://dl.bintray.com/whisk/maven",
      scalacOptions ++= Seq("-feature", "-deprecation")
    )
}
