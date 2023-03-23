package com.contentsquare.endpoint

import zio.http._
import zio.http.model.Method

object PingEndpoint {
  def apply(): Http[Any, Nothing, Request, Response] =
    Http.collect[Request] {
      // GET /ping
      case Method.GET -> !! / "ping" => Response.ok
    }
}
