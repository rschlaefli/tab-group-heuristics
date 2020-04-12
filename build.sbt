import Dependencies._

ThisBuild / version := "0.0.1"
ThisBuild / scalaVersion := "2.13.1"
ThisBuild / organization := "ch.uzh.rschlaefli"

lazy val tabs = (project in file("."))
  .settings(
    name := "Tabs",
    libraryDependencies ++= runtimeDependencies,
    libraryDependencies ++= testingDependencies
  )

// ensure that only the main entrypoint is bundled into the package
mainClass in Compile := Some("main.Main")
discoveredMainClasses in Compile := Seq()

enablePlugins(JavaAppPackaging)
