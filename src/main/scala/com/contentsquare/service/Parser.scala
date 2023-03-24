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

    /**
     * Returns an effect that will parse [[Body]] given in input and produce a A
     * type. It requires that implicit decoder for the given type is declared.
     * It may fail with [[InvalidInputDataException]] if there is an error
     * during parsing.
     */
    override def parseBody[A](body: Body)(implicit decoder: Decoder[A]): ZIO[Any, Throwable, A] =
      (for {
        json <- body.asString
        obj  <- ZIO
          .fromEither(decode[A](json))
      } yield obj)
        .mapError(error => InvalidInputDataException(s"Error on parsing body ${error.getMessage}"))
  }

  /**
   * Returns an effect that requires [[Parser]] layer to be provided and call
   * [[parseBody]].
   */
  def parseBody[A](body: Body)(implicit decoder: Decoder[A]): ZIO[Parser, Throwable, A] =
    ZIO.serviceWithZIO[Parser](_.parseBody[A](body))

  /**
   * Parser default layer
   */
  val layer: ZLayer[Any, Nothing, Parser] = ZLayer.succeed(LiveService())
}
