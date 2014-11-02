name := """mer"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  //play-mongo-jackson-mapper,
  //"org.webjars" % "webjars-play" % "2.11.0",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.2"
  //"org.reactivemongo" %% "play2-reactivemongo" % "0.10.2"
  // WebJars pull in client-side web libraries
  //"org.webjars" %% "webjars-play" % "2.1.0",
  //"org.webjars" % "jquery" % "2.1.0"
  //"net.vz.mongodb.jackson" %% "play-mongo-jackson-mapper" % "1.3.0"
)
