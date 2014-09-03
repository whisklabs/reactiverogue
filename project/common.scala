import sbt._
import Keys._
import com.typesafe.sbt.SbtScalariform.scalariformSettings

object common {

  val gitHeadCommitSha = settingKey[String]("current git commit SHA")

  val commonSettings = Seq(
    organization := "com.whisk",
    scalaVersion := "2.11.2",
    crossScalaVersions := Seq("2.11.2", "2.10.4"),
    gitHeadCommitSha := Process("git rev-parse --short HEAD").lines.head)
    
  def module(name: String) =
    Project(name, file(name))
    .settings(commonSettings:_*)
    .settings(scalariformSettings:_*)
    .settings(
      scalacOptions ++= Seq("-feature", "-deprecation"),
      version := "0.1.0-" + gitHeadCommitSha.value,
      resolvers ++= Seq(
        "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
         "typesafe" at "http://repo.typesafe.com/typesafe/releases/"),
      publishTo := {
        val dir = if (version.value.trim.endsWith(gitHeadCommitSha.value)) "snapshots" else "releases"
        val repo = Path.userHome / "mvn-repo" / dir
        Some(Resolver.file("file", repo) )
      }
    )
}
