import Dependencies._

ThisBuild / version := "0.0.1"
ThisBuild / scalaVersion := "2.13.2"
ThisBuild / organization := "ch.uzh.rschlaefli"

lazy val tabs = (project in file("."))
  .settings(
    name := "automated-tab-grouping",
    maintainer := "rolandschlaefli@gmail.com",
    libraryDependencies ++= runtimeDependencies,
    libraryDependencies ++= testingDependencies
  )

// ensure that only the main entrypoint is bundled into the package
mainClass in Compile := Some("main.Main")
discoveredMainClasses in Compile := Seq()

// ensure that javadoc is not built on every stage
// ref: https://sbt-native-packager.readthedocs.io/en/stable/formats/universal.html#skip-packagedoc-task-on-stage
mappings in (Compile, packageDoc) := Seq()

// embed additional files in the package
mappings in (Universal, packageZipTarball) += file(
  "deployment/manifest-chrome.json"
) -> "manifest-chrome.json"
mappings in (Universal, packageZipTarball) += file(
  "deployment/manifest-firefox.json"
) -> "manifest-firefox.json"
mappings in (Universal, packageZipTarball) += file(
  "deployment/install-windows.bat"
) -> "install.bat"
mappings in (Universal, packageZipTarball) += file(
  "deployment/install-linux.sh"
) -> "install-linux.sh"
mappings in (Universal, packageZipTarball) += file(
  "deployment/install-mac.sh"
) -> "install-mac.sh"
// mappings in (Universal, packageOsxDmg) += file("scripts/install-linux.sh") -> "install.sh"

enablePlugins(JavaAppPackaging)
enablePlugins(UniversalPlugin)
