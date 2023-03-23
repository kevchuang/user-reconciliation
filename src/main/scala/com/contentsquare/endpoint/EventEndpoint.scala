package com.contentsquare.endpoint

import com.contentsquare.database.Database
import com.contentsquare.error.DatabaseError
import com.contentsquare.error.DatabaseError.InvalidInput
import com.contentsquare.model.{Event, UpdateEvent}
import io.circe.Decoder
import io.circe.parser.decode
import zio._
import zio.http._
import zio.http.model.Method
import zio.stream.{ZSink, ZStream}

object EventEndpoint {

  private[this] def parseBody[A](body: Body)(implicit decoder: Decoder[A]): ZIO[Any, InvalidInput, A] =
    (for {
      json <- body.asString
      obj  <- ZIO.fromEither(decode[A](json))
    } yield obj).logError
      .mapError(error => InvalidInput(error.getMessage))

  private[this] def insertEventRequest(request: Request): ZIO[Database, DatabaseError, Response] =
    for {
      event          <- parseBody[Event](request.body)
      validatedEvent <- event.validateEvent
      _              <- Database.insertEvent(validatedEvent)
    } yield Response.ok

  private[this] def insertEventSink(): ZSink[Database, DatabaseError, Request, Request, Response] =
    ZSink
      .take[Request](1)
      .map(_.headOption)
      .mapZIO {
        case Some(request) => insertEventRequest(request).logError
        case None          => ZIO.succeed(Response.ok)
      }

  private[this] def updateEventRequest(request: Request): ZIO[Database, DatabaseError, Response] =
    for {
      event          <- parseBody[UpdateEvent](request.body)
      validatedEvent <- event.validateUpdateEvent
      _              <- Database.updateEvent(validatedEvent)
    } yield Response.ok

  private[this] def updateEventSink(): ZSink[Database, DatabaseError, Request, Request, Response] =
    ZSink
      .take[Request](1)
      .map(_.headOption)
      .mapZIO {
        case Some(request) => updateEventRequest(request).logError
        case None          => ZIO.succeed(Response.ok)
      }

  def apply(): App[Database] = {
    Http
      .collectZIO[Request] {
        // POST /collect
        case request @ Method.POST -> !! / "collect" =>
          ZStream(request)
            .run(insertEventSink())
        // POST /update
        case request @ Method.POST -> !! / "update"  =>
          ZStream(request)
            .schedule(Schedule.spaced(400.milliseconds))
            .run(updateEventSink())
      }
      .mapError(_ => Response.ok)
  }
}
