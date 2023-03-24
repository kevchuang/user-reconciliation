package com.contentsquare

import com.contentsquare.database.InMemoryDatabase
import com.contentsquare.server.HttpServer
import com.contentsquare.service.Parser
import zio._

object MainApp extends ZIOAppDefault {

  override def run: ZIO[Any, Throwable, Unit] =
    HttpServer.start
      .provide(HttpServer.layer, Parser.layer, InMemoryDatabase.layer)

}
