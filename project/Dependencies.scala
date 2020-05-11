import sbt._

object Dependencies {
  val scalatestVersion = "3.1.1"

  val scalaz = "org.scalaz" %% "scalaz-core" % "7.2.30"
  val scalactic = "org.scalactic" %% "scalactic" % scalatestVersion
  val scalatest = "org.scalatest" %% "scalatest" % scalatestVersion
  val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"

  val scalaGraph = "org.scala-graph" %% "graph-core" % "1.13.2"
  val scalaGraphJson = "org.scala-graph" %% "graph-json" % "1.13.0"
  val scalaGraphDot = "org.scala-graph" %% "graph-dot" % "1.13.0"

  val circeVersion = "0.12.3"

  val smile = "com.github.haifengl" %% "smile-scala" % "2.3.0"

  val runtimeDependencies =
    Seq(
      scalaz,
      scalactic,
      logback,
      scalaLogging,
      scalaGraph,
      scalaGraphJson,
      scalaGraphDot,
      smile
    ) ++ Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion)
  val testingDependencies = Seq(scalatest % "test")
}
