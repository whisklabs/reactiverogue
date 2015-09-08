import sbt._

object dependencies {

  object V {
    val Reactivemongo = "0.11.7"
  }

  val playJson = "com.typesafe.play" %% "play-json" % "2.3.9"
  val reactiveMongo = "org.reactivemongo" %% "reactivemongo" % V.Reactivemongo
  val playReactivemongo = "org.reactivemongo" %% "play2-reactivemongo" % "0.11.7.play23"

  val jodaTime = "joda-time" % "joda-time" % "2.8.1"
  val jodaConvert = "org.joda" % "joda-convert" % "1.7"

  val junit = "junit" % "junit" % "4.12"
  val junitInterface = "com.novocode" % "junit-interface" % "0.6"
  val specs2 = "org.specs2" %% "specs2-junit" % "2.3.11"

  val mongoDependencies =
    Seq(reactiveMongo, jodaTime)

  val recordDependencies =
    Seq(playJson, playReactivemongo)

  val coreDependencies =
    Seq(playJson, playReactivemongo)

  val recordDslDependencies =
    Seq(junit % "test", specs2 % "test", junitInterface % "test")
}
