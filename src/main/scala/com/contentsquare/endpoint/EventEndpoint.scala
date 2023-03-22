package com.contentsquare.endpoint

import com.contentsquare.database.Database
import com.contentsquare.error.DatabaseError
import com.contentsquare.error.DatabaseError.InvalidInput
import com.contentsquare.model.{Event, UpdateEvent}
import io.circe.Decoder
import io.circe.parser.decode
import io.circe.syntax._
import zio._
import zio.http._
import zio.http.model.{HTTP_CHARSET, Method}
import zio.stream.ZSink

object EventEndpoint {

  private[this] def parseBody[A](body: Body)(implicit decoder: Decoder[A]): ZIO[Any, InvalidInput, A] =
    (for {
      json <- body.asString
      obj  <- ZIO.fromEither(decode[A](json))
    } yield obj).logError
      .mapError(error => InvalidInput(error.getMessage))

  def getEvent: ZIO[Database, Throwable, Response] =
    for {
      events <- Database.getUserEvents
    } yield Response.json(events.asJson.noSpaces)

  def insertEvent(request: Request): ZIO[Database, DatabaseError, Response] =
    for {
      event          <- parseBody[Event](request.body)
      validatedEvent <- event.validateEvent
      _              <- Database.insertEvent(validatedEvent)
    } yield Response.ok

  def insertEvents(request: Request): ZIO[Database, DatabaseError, Response] =
    for {
      events          <- parseBody[List[Event]](request.body)
      validatedEvents <- ZIO.collectAll(events.map(_.validateEvent))
      _               <- ZIO.collectAll(validatedEvents.map(Database.insertEvent))
    } yield Response.ok

  def updateEvent(request: Request): ZIO[Database, DatabaseError, Response] =
    for {
      event          <- parseBody[UpdateEvent](request.body)
      validatedEvent <- event.validateUpdateEvent
      _              <- Database.updateEvent(validatedEvent)
    } yield Response.ok

  def updateEvents(request: Request): ZIO[Database, DatabaseError, Response] =
    for {
      events         <- parseBody[List[UpdateEvent]](request.body)
      validatedEvent <- ZIO.collectAll(events.map(_.validateUpdateEvent))
      _              <- ZIO.collectAll(validatedEvent.map(Database.updateEvent))
    } yield Response.ok

  def apply(): App[Database] = {
    val sink: ZSink[Database, DatabaseError, Chunk[Byte], Chunk[Byte], Response] =
      ZSink
        .take(1)
        .mapZIO { bytes =>
          (for {
            event <- ZIO.fromEither(decode[Event](bytes.flatten.asString(HTTP_CHARSET)))
            _     <- Database.insertEvent(event)
          } yield Response.ok).logError
            .mapError(_ => InvalidInput("Invalid output"))
        }

    val sinkUpdate: ZSink[Database, DatabaseError, Chunk[Byte], Chunk[Byte], Response] =
      ZSink
        .take(1)
        .mapZIO { bytes =>
          (for {
            event <- ZIO.fromEither(decode[UpdateEvent](bytes.flatten.asString(HTTP_CHARSET)))
            _     <- Database.updateEvent(event)
          } yield Response.ok).logError
            .mapError(_ => InvalidInput("Invalid output"))
        }

    Http
      .collectZIO[Request] {
        case Method.GET -> !! / "events"             => getEvent
        case request @ Method.POST -> !! / "collect" =>
          request
            .body
            .asStream
            .chunks
            .run(sink)
        case request @ Method.POST -> !! / "update"  =>
          request
            .body
            .asStream
            .chunks
            .schedule(Schedule.spaced(500.milliseconds))
            .run(sinkUpdate)
      }
      .mapError(_ => Response.ok)
  }
}
