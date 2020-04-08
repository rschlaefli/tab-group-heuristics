import Dependencies._

ThisBuild / version := "0.0.1"
ThisBuild / scalaVersion := "2.13.1"
ThisBuild / organization := "ch.uzh.rschlaefli"

lazy val collector = (project in file("."))
  .settings(
    name := "Collector",
    libraryDependencies ++= runtimeDependencies,
    libraryDependencies ++= testingDependencies
  )

enablePlugins(JavaAppPackaging)
