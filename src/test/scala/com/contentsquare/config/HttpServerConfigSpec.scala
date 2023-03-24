package com.contentsquare.config

import zio.Scope
import zio.http.ServerConfig
import zio.test.Assertion._
import zio.test._

object HttpServerConfigSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment with Scope, Any] =
    loadConfigSuite + createServerConfigSuite

  private lazy val loadConfigSuite =
    suite("loadConfig")(
      test("should read in resources path and return HttpServerConfig") {
        val expected = HttpServerConfig("0.0.0.0", 8080)

        assertZIO(HttpServerConfig.loadConfig)(equalTo(expected))
      }
    )

  private lazy val createServerConfigSuite =
    suite("createServerConfig")(
      test("should return a ServerConfig") {
        val expected = ServerConfig.default.binding("0.0.0.0", 8080)

        assertZIO(HttpServerConfig.createServerConfig)(equalTo(expected))
      }
    )

}
