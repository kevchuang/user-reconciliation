import BuildHelper._
import Dependencies._

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(nameSettings)
  .settings(standardSettings)
  .settings(
    libraryDependencies ++= Seq(
      `circe-core`,
      `circe-generic`,
      `circe-parser`,
      zio,
      `zio-config`,
      `zio-config-typesafe`,
      `zio-config-magnolia`,
      `zio-http`,
      `zio-test`,
      `zio-test-sbt`
    )
  )
