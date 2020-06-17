import sbt._

object Dependencies {
  val scalatestVersion = "3.1.2"

  val scalaz = "org.scalaz" %% "scalaz-core" % "7.3.1"
  val scalactic = "org.scalactic" %% "scalactic" % scalatestVersion
  val scalatest = "org.scalatest" %% "scalatest" % scalatestVersion

  val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
  val janino = "org.codehaus.janino" % "janino" % "3.1.2"

  val circeVersion = "0.13.0"
  val circe = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % circeVersion)

  val smileCore = "com.github.haifengl" % "smile-core" % "2.4.0"
  val smileScala = "com.github.haifengl" %% "smile-scala" % "2.4.0"

  val nscalaTime = "com.github.nscala-time" %% "nscala-time" % "2.24.0"

  val jgraphVersion = "1.4.0"
  val jgraph = Seq(
    "org.jgrapht" % "jgrapht-core",
    "org.jgrapht" % "jgrapht-ext",
    "org.jgrapht" % "jgrapht-io",
    "org.jgrapht" % "jgrapht-opt"
  ).map(_ % jgraphVersion)

  val runtimeDependencies =
    Seq(
      scalaz,
      scalactic,
      logback,
      scalaLogging,
      janino,
      smileCore,
      smileScala,
      nscalaTime
    ) ++ circe ++ jgraph
  val testingDependencies = Seq(scalatest % "test")
}
