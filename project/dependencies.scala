import sbt._

object dependencies {

  object V {
    val Reactivemongo = "0.11.14"
  }

  val playJson = "com.typesafe.play" %% "play-json" % "2.5.4"
  val reactivemongoBson = "org.reactivemongo" %% "reactivemongo-bson" % V.Reactivemongo
  val reactivemongoIteratees = "org.reactivemongo" %% "reactivemongo-iteratees" % V.Reactivemongo
  val reactivemongoJson = "org.reactivemongo" %% "reactivemongo-play-json" % (V.Reactivemongo )
  val reactivemongo = "org.reactivemongo" %% "reactivemongo" % V.Reactivemongo

  val junit = "junit" % "junit" % "4.12"
  val scalatest = "org.scalatest" %% "scalatest" % "2.2.6"
  val dockerIt = "com.whisk" %% "docker-testkit-scalatest" % "0.8.2"

  val bsonDependencies =
    Seq(reactivemongoBson)

  val coreDependencies =
    Seq(reactivemongo, reactivemongoIteratees, reactivemongoJson, playJson)

  val recordDslDependencies =
    Seq(junit % "test", scalatest % "test", dockerIt % "test")
}
