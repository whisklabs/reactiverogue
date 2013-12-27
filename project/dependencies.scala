import sbt._

object Dependencies {
  
  object V {
    val Rogue = "2.2.0"
  }

  val playJson = "com.typesafe.play" %% "play-json" % "2.2.1"
  val playReactiveMongo = "org.reactivemongo" %% "play2-reactivemongo" % "0.10.0"
  val reactiveMongo = "org.reactivemongo" %% "reactivemongo" % "0.10.0"
  
  val rogueField = "com.foursquare" %% "rogue-field" % V.Rogue intransitive()
  val rogueIndex = "com.foursquare" %% "rogue-index" % V.Rogue intransitive()
  
  val jodaTime = "joda-time" % "joda-time" % "2.1"
  val jodaConvert = "org.joda" % "joda-convert" % "1.4"
  
  val junit = "junit" % "junit" % "4.5"
  val junitInterface = "com.novocode" % "junit-interface" % "0.6"
  val specs2 = "org.specs2" %% "specs2" % "1.12.3"
  
  val mongoDependencies =
    Seq(reactiveMongo, jodaTime, jodaConvert)

  val recordDependencies =
    Seq(playReactiveMongo, playJson)
    
  val coreDependencies =
    Seq(rogueField, rogueIndex, playReactiveMongo, playJson)
    
  val recordDslDependencies =
    Seq(junit % "test", specs2 % "test", junitInterface % "test")
}
