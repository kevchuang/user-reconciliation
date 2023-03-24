package com.contentsquare.service

import com.contentsquare.error.Errors.InvalidInputDataException
import io.circe.Decoder
import io.circe.parser.decode
import zio.{ZIO, ZLayer}
import zio.http.Body

object Parser {
  type Parser = Service

  trait Service {
    def parseBody[A](body: Body)(implicit decoder: Decoder[A]): ZIO[Any, Throwable, A]
  }

  final case class LiveService() extends Parser {
    override def parseBody[A](body: Body)(implicit decoder: Decoder[A]): ZIO[Any, Throwable, A] =
      (for {
        json <- body.asString
        obj  <- ZIO
          .fromEither(decode[A](json))
      } yield obj)
        .mapError(error => InvalidInputDataException(s"Error on parsing body ${error.getMessage}"))
  }

  def parseBody[A](body: Body)(implicit decoder: Decoder[A]): ZIO[Parser, Throwable, A] =
    ZIO.serviceWithZIO[Parser](_.parseBody[A](body))

  val layer: ZLayer[Any, Nothing, Parser] = ZLayer.succeed(LiveService())
}
