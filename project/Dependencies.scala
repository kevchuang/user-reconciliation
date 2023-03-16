import sbt._

object Dependencies {
  val zioHttpVersion = "0.0.5"
  val zioVersion     = "2.0.10"

  val zio            = "dev.zio" %% "zio"          % zioVersion
  val `zio-http`     = "dev.zio" %% "zio-http"     % zioHttpVersion
  val `zio-streams`  = "dev.zio" %% "zio-streams"  % zioVersion
  val `zio-test`     = "dev.zio" %% "zio-test"     % zioVersion % Test
  val `zio-test-sbt` = "dev.zio" %% "zio-test-sbt" % zioVersion % Test
}
