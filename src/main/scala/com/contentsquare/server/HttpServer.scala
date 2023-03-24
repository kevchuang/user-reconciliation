package com.contentsquare.server

import com.contentsquare.config.HttpServerConfig
import com.contentsquare.database.Database
import com.contentsquare.endpoint.{EventEndpoint, MetricsEndpoint, PingEndpoint}
import com.contentsquare.service.Parser.Parser
import zio.http._
import zio._

object HttpServer {

  private lazy val routes =
    PingEndpoint() ++ EventEndpoint() ++ MetricsEndpoint()

  def start: ZIO[Any & Database & Server & Parser, Throwable, Unit] =
    for {
      _      <- ZIO.logInfo("Starting http server")
      server <- Server.serve(routes @@ HttpAppMiddleware.debug).fork
      _      <- Console.readLine("Press ENTER to interrupt the server\n")
      _      <- Console.printLine("Interrupting server")
      _      <- server.interrupt
    } yield ()

  val layer: ZLayer[Any, Throwable, Server] =
    HttpServerConfig.layer >>> Server.live
}
