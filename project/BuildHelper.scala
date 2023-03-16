import sbt.Keys._
import sbt._
import scalafix.sbt.ScalafixPlugin.autoImport._

object BuildHelper extends ScalaSettings {
  val scala213 = "2.13.10"

  def nameSettings = Seq(
    name             := "user-reconciliation",
    organization     := "com.contentsquare",
    organizationName := "contentsquare",
  )

  def standardSettings = Seq(
    ThisBuild / scalaVersion               := scala213,
    scalacOptions                          := baseSettings,
    semanticdbVersion                      := scalafixSemanticdb.revision, // use Scalafix compatible version
    ThisBuild / scalafixScalaBinaryVersion := CrossVersion.binaryScalaVersion(scala213),
    ThisBuild / scalafixDependencies ++=
      List(
        "com.github.liancheng" %% "organize-imports" % "0.6.0",
        "com.github.vovapolu"  %% "scaluzzi"         % "0.1.23",
      ),
    Test / parallelExecution               := true,
    ThisBuild / fork                       := true,
  )
}
