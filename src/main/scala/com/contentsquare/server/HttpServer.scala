package com.contentsquare.server

import com.contentsquare.config.HttpServerConfig
import zio.http._
import zio.http.model.Method
import zio.{ZIO, ZLayer}

object HttpServer {

  val app: HttpApp[Any, Nothing] =
    Http.collect[Request] { case Method.GET -> !! / "text" =>
      Response.text("Hello World!")
    }

  def start: ZIO[Any with Server, Throwable, Unit] =
    for {
      _ <- ZIO.logInfo("Starting http server")
      _ <- Server.serve(HttpServer.app)
    } yield ()

  val layer: ZLayer[Any, Throwable, Server] =
    HttpServerConfig.layer >>> Server.live
}
