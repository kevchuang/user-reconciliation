import BuildHelper._
import Dependencies._

ThisBuild / scalaVersion     := "2.13.10"
ThisBuild / organization     := "com.contentsquare"
ThisBuild / organizationName := "contentsquare"

lazy val root = (project in file("."))
  .settings(nameSettings)
  .settings(standardSettings)
  .settings(
    libraryDependencies ++= Seq(
      zio,
      `zio-http`,
      `zio-streams`,
      `zio-test`,
      `zio-test-sbt`
    )
  )
