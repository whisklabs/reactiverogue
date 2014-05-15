import sbt._

object dependencies {
  
  object V {
    val Reactivemongo = "0.11.0-SNAPSHOT"
  }

  val playJson = "com.typesafe.play" %% "play-json" % "2.3.0-RC1"
  val reactiveMongo = "org.reactivemongo" %% "reactivemongo" % V.Reactivemongo
  
  val jodaTime = "joda-time" % "joda-time" % "2.1"
  val jodaConvert = "org.joda" % "joda-convert" % "1.4"
  
  val junit = "junit" % "junit" % "4.5"
  val junitInterface = "com.novocode" % "junit-interface" % "0.6"
  val specs2 = "org.specs2" %% "specs2-junit" % "2.3.11"
  
  val mongoDependencies =
    Seq(reactiveMongo, jodaTime, jodaConvert)

  val recordDependencies =
    Seq(playJson)
    
  val coreDependencies =
    Seq(playJson)
    
  val recordDslDependencies =
    Seq(junit % "test", specs2 % "test", junitInterface % "test")
}
