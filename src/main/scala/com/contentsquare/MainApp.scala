package com.contentsquare

import com.contentsquare.server.HttpServer
import zio._

object MainApp extends ZIOAppDefault {

  override def run: ZIO[Any, Throwable, Unit] =
    HttpServer.start
      .provide(
        HttpServer.layer
      )

}
