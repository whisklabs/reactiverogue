import sbt._

object Dependencies {
  
  object V {
    val Lift = "2.5.1"
    val Rogue = "2.2.0"
  }

  val liftJson = "net.liftweb" %% "lift-json" % V.Lift
  
  val reactiveMongo = "org.reactivemongo" %% "reactivemongo" % "0.9"
  
  val rogueField = "com.foursquare" %% "rogue-field" % V.Rogue intransitive()
  val rogueIndex = "com.foursquare" %% "rogue-index" % V.Rogue intransitive()
  
  val jodaTime = "joda-time" % "joda-time" % "2.1"
  val jodaConvert = "org.joda" % "joda-convert" % "1.4"
  
  val junit = "junit" % "junit" % "4.5"
  val junitInterface = "com.novocode" % "junit-interface" % "0.6"
  val specs2 = "org.specs2" %% "specs2" % "1.12.3"
  
  val mongoDependencies =
    Seq(reactiveMongo, jodaTime, jodaConvert, liftJson)
    
  val coreDependencies =
    Seq(rogueField, rogueIndex)
    
  val recordDslDependencies =
    Seq(junit % "test", specs2 % "test", junitInterface % "test")
}
