package com.contentsquare.config

import zio._
import zio.config.magnolia._
import zio.config.typesafe._
import zio.http.ServerConfig

final case class HttpServerConfig(
  host: String,
  port: Int
)

object HttpServerConfig {

  /**
   * Returns an effect that will read application.conf in resources path and
   * produces a [[HttpServerConfig]]
   */
  private[config] def loadConfig: IO[Config.Error, HttpServerConfig] =
    ConfigProvider.fromResourcePath
      .load(deriveConfig[HttpServerConfig])

  /**
   * Returns an effect that will load configuration file and produce a [[ServerConfig]]
   */
  private[config] def createServerConfig: ZIO[Any, Config.Error, ServerConfig] =
    for {
      config <- loadConfig
      _      <- ZIO.logInfo(
        "Application started with following configuration:\n" +
          s"\thost: ${config.host}\n" +
          s"\tport: ${config.port}"
      )
    } yield ServerConfig.default.binding(config.host, config.port)

  /**
   * [[ServerConfig]] default layer
   */
  val layer: ZLayer[Any, Config.Error, ServerConfig] =
    ZLayer.fromZIO(createServerConfig)

}
