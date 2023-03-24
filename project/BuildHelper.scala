import sbt.Keys._
import sbt._
import scalafix.sbt.ScalafixPlugin.autoImport._

object BuildHelper extends ScalaSettings {
  val scala213 = "2.13.10"

  def nameSettings = Seq(
    name             := "user-reconciliation",
    organization     := "com.contentsquare",
    organizationName := "contentsquare"
  )

  def standardSettings = Seq(
    ThisBuild / scalaVersion := scala213,
    scalacOptions            := baseSettings,
    Test / parallelExecution := true,
    ThisBuild / fork         := true,
    run / fork               := true,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
}
