import play.PlayImport.PlayKeys
import play.PlayImport.PlayKeys._

name := """mer"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.4"

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

resolvers += "pk11 repo" at "http://pk11-scratch.googlecode.com/svn/trunk"

routesImport ++= Seq("scala.language.reflectiveCalls")

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.0.akka23",
  "ws.securesocial" %% "securesocial" % "3.0-M1",
  "com.typesafe.play.plugins" %% "play-plugins-redis" % "2.3.1"
)




