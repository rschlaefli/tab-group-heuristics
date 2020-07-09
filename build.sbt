import Dependencies._

Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild(
  List(
    scalaVersion := "2.13.2",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.3.1-RC3"
  )
)

lazy val tabs = (project in file("."))
  .settings(
    name := "tabs",
    version := "0.3.1",
    organization := "ch.uzh.rschlaefli",
    maintainer := "rolandschlaefli@gmail.com",
    packageSummary := "Automated Tab Organization",
    packageDescription := """Heuristics backend for the Automated Tab Organization WebExtension""",
    wixProductId := "25695a01-bd23-4155-a430-6ef8be4babfa",
    wixProductUpgradeId := "9b2f86fa-aa0d-4b12-ac73-6831f4628329",
    libraryDependencies ++= runtimeDependencies,
    libraryDependencies ++= testingDependencies,
    scalacOptions += "-Wunused"
  )

// ensure that only the main entrypoint is bundled into the package
mainClass in Compile := Some("main.Main")
discoveredMainClasses in Compile := Seq()

// ensure that javadoc is not built on every stage
// ref: https://sbt-native-packager.readthedocs.io/en/stable/formats/universal.html#skip-packagedoc-task-on-stage
mappings in (Compile, packageDoc) := Seq()

// embed additional files in the package
mappings in (Universal, packageZipTarball) += file(
  "deployment/manifest-chrome.json.template"
) -> "manifest-chrome.json.template"
mappings in (Universal, packageZipTarball) += file(
  "deployment/manifest-firefox.json.template"
) -> "manifest-firefox.json.template"
mappings in (Universal, packageZipTarball) += file(
  "deployment/clean-unix.sh"
) -> "clean-unix.sh"
mappings in (Universal, packageZipTarball) += file(
  "deployment/install-linux.sh"
) -> "install-linux.sh"
mappings in (Universal, packageZipTarball) += file(
  "deployment/install-mac.command"
) -> "install-mac.command"
// mappings in (Universal, packageOsxDmg) += file("scripts/install-mac.command") -> "install-mac.command"

enablePlugins(JavaAppPackaging)
enablePlugins(UniversalPlugin)
enablePlugins(WindowsPlugin)
