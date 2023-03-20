package com.contentsquare.server

import com.contentsquare.config.HttpServerConfig
import com.contentsquare.database.Database
import com.contentsquare.endpoint.{EventEndpoint, MetricsEndpoint, PingEndpoint}
import zio.http._
import zio.{&, ZIO, ZLayer}

object HttpServer {

  def start: ZIO[Any & Database & Server, Nothing, Unit] =
    for {
      _ <- ZIO.logInfo("Starting http server")
      _ <- Server.serve(
        (PingEndpoint() ++ EventEndpoint() ++ MetricsEndpoint()) @@ HttpAppMiddleware.debug
      )
    } yield ()

  val layer: ZLayer[Any, Throwable, Server] =
    HttpServerConfig.layer >>> Server.live
}
