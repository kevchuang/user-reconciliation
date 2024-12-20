import sbt._

object Dependencies {
  val circeVersion     = "0.14.5"
  val zioConfigVersion = "4.0.0-RC12"
  val zioHttpVersion   = "0.0.5"
  val zioJsonVersion   = "0.4.2"
  val zioVersion       = "2.0.10"

  val `circe-core`          = "io.circe" %% "circe-core"          % circeVersion
  val `circe-generic`       = "io.circe" %% "circe-generic"       % circeVersion
  val `circe-parser`        = "io.circe" %% "circe-parser"        % circeVersion
  val zio                   = "dev.zio"  %% "zio"                 % zioVersion
  val `zio-config`          = "dev.zio"  %% "zio-config"          % zioConfigVersion
  val `zio-config-magnolia` = "dev.zio"  %% "zio-config-magnolia" % zioConfigVersion
  val `zio-config-typesafe` = "dev.zio"  %% "zio-config-typesafe" % zioConfigVersion
  val `zio-http`            = "dev.zio"  %% "zio-http"            % zioHttpVersion
  val `zio-test`            = "dev.zio"  %% "zio-test"            % zioVersion % Test
  val `zio-test-sbt`        = "dev.zio"  %% "zio-test-sbt"        % zioVersion % Test
}
