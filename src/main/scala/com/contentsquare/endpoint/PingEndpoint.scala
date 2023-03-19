package com.contentsquare.endpoint

import zio.http._
import zio.http.model.Method

object PingEndpoint {
  def apply(): Http[Any, Nothing, Request, Response] =
    Http.collect[Request] { case Method.GET -> !! / "ping" =>
      Response.ok
    }
}
