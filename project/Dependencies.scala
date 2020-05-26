import sbt._

object Dependencies {
  val scalatestVersion = "3.1.2"

  val scalaz = "org.scalaz" %% "scalaz-core" % "7.3.1"
  val scalactic = "org.scalactic" %% "scalactic" % scalatestVersion
  val scalatest = "org.scalatest" %% "scalatest" % scalatestVersion

  val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
  val janino = "org.codehaus.janino" % "janino" % "3.1.2"

  val scalaGraph = "org.scala-graph" %% "graph-core" % "1.13.2"
  val scalaGraphJson = "org.scala-graph" %% "graph-json" % "1.13.0"
  val scalaGraphDot = "org.scala-graph" %% "graph-dot" % "1.13.0"

  val circeVersion = "0.13.0"

  val smileCore = "com.github.haifengl" % "smile-core" % "2.4.0"
  val smileScala = "com.github.haifengl" %% "smile-scala" % "2.4.0"

  val nscalaTime = "com.github.nscala-time" %% "nscala-time" % "2.24.0"

  val runtimeDependencies =
    Seq(
      scalaz,
      scalactic,
      logback,
      scalaLogging,
      janino,
      scalaGraph,
      scalaGraphJson,
      scalaGraphDot,
      smileCore,
      smileScala,
      nscalaTime
    ) ++ Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion)
  val testingDependencies = Seq(scalatest % "test")
}
