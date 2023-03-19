package com.contentsquare

import com.contentsquare.database.InMemoryDatabase
import com.contentsquare.server.HttpServer
import zio._

object MainApp extends ZIOAppDefault {

  override def run: ZIO[Any, Throwable, Unit] =
    HttpServer
      .start
      .provide(InMemoryDatabase.layer, HttpServer.layer)

}
