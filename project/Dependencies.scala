import sbt._

object Dependencies {
  val scalatestVersion = "3.1.1"

  val scalaz = "org.scalaz" %% "scalaz-core" % "7.2.30"
  val scalactic = "org.scalactic" %% "scalactic" % scalatestVersion
  val scalatest = "org.scalatest" %% "scalatest" % scalatestVersion
  val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
  val circeVersion = "0.12.3"

  val runtimeDependencies =
    Seq(scalaz, scalactic, logback, scalaLogging) ++ Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion)
  val testingDependencies = Seq(scalatest % "test")
}
