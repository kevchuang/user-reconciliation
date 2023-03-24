package com.contentsquare.server

import com.contentsquare.config.HttpServerConfig
import com.contentsquare.database.Database
import com.contentsquare.endpoint.{EventEndpoint, MetricsEndpoint, PingEndpoint}
import com.contentsquare.service.Parser.Parser
import zio.http._
import zio._

object HttpServer {

  /**
   * Routes that will be used in HttpServer.start
   */
  private lazy val routes: Http[Database & Parser, Response, Request, Response] =
    PingEndpoint() ++ EventEndpoint() ++ MetricsEndpoint()

  /**
   * Start a server binding on host and port indicated in
   * resources/application.conf with specific routes defined in HttpServer.
   */
  def start: ZIO[Any & Database & Server & Parser, Throwable, Unit] =
    for {
      _ <- ZIO.logInfo("Starting http server")
      _ <- Server.serve(routes @@ HttpAppMiddleware.debug)
    } yield ()

  /**
   * Server layer that load config to bind the server
   */
  val layer: ZLayer[Any, Throwable, Server] =
    HttpServerConfig.layer >>> Server.live
}
