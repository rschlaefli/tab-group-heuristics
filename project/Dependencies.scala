import sbt._

object Dependencies {
  val scalatestVersion = "3.1.1"

  val scalaz = "org.scalaz" %% "scalaz-core" % "7.2.30"
  val scalactic = "org.scalactic" %% "scalactic" % scalatestVersion
  val scalatest = "org.scalatest" %% "scalatest" % scalatestVersion
  val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
  val argonautJson = "io.argonaut" %% "argonaut" % "6.2.5"

  val runtimeDependencies =
    Seq(scalaz, scalactic, logback, scalaLogging, argonautJson)
  val testingDependencies = Seq(scalatest % "test")
}
