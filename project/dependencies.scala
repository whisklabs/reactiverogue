import sbt._

object dependencies {

  object V {
    val Reactivemongo = "0.11.7"
  }

  val playJson = "com.typesafe.play" %% "play-json" % "2.4.3"
  val reactivemongoBson = "org.reactivemongo" %% "reactivemongo-bson" % V.Reactivemongo
  val reactivemongo = "org.reactivemongo" %% "reactivemongo" % V.Reactivemongo

  val junit = "junit" % "junit" % "4.12"
  val junitInterface = "com.novocode" % "junit-interface" % "0.6"
  val specs2 = "org.specs2" %% "specs2-junit" % "2.3.11"

  val bsonDependencies =
    Seq(reactivemongoBson)

  val jsonDependencies =
    Seq(reactivemongo, playJson)

  val coreDependencies =
    Seq(reactivemongo)

  val recordDslDependencies =
    Seq(junit % "test", specs2 % "test", junitInterface % "test")
}
