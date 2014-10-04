/**
 * Created by wayneyu on 10/4/14.
 */
import sbt._
import play.

object ApplicationBuild extends Build {

  val appName         = "MERAPP"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "org.webjars" % "webjars-play" % "2.1.0",
    "org.webjars" % "jquery" % "1.9.1",
    "net.vz.mongodb.jackson" %% "play-mongo-jackson-mapper" % "1.1.0"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(

  )

}
