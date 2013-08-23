import sbt._
import Keys._
import com.typesafe.sbt.SbtScalariform.scalariformSettings

object common {

  val commonSettings = Seq(
    organization := "whisk",
    scalaVersion := "2.10.2")
    
  def module(name: String) =
    Project(name, file(name))
    .settings(commonSettings:_*)
    .settings(scalariformSettings:_*)
}