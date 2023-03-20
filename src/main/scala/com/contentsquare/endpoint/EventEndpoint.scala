package com.contentsquare.endpoint

import com.contentsquare.database.Database
import com.contentsquare.error.DatabaseError
import com.contentsquare.error.DatabaseError.InvalidInput
import com.contentsquare.model.{Event, UpdateEvent}
import io.circe.Decoder
import io.circe.parser.decode
import io.circe.syntax._
import zio.ZIO
import zio.http._
import zio.http.model.Method

object EventEndpoint {

  private[this] def parseBody[A](body: Body)(implicit decoder: Decoder[A]): ZIO[Any, InvalidInput, A] =
    (for {
      json <- body.asString
      obj  <- ZIO.fromEither(decode[A](json))
    } yield obj).logError
      .mapError(error => InvalidInput(error.getMessage))

  private def getEvent: ZIO[Database, Throwable, Response] =
    for {
      events <- Database.getUserEvents
    } yield Response.json(events.asJson.noSpaces)

  private def insertEvent(request: Request): ZIO[Database, DatabaseError, Response] =
    for {
      event          <- parseBody[Event](request.body)
      validatedEvent <- event.validateEvent
      _              <- Database.insertEvent(validatedEvent)
    } yield Response.ok

  private def insertEvents(request: Request): ZIO[Database, DatabaseError, Response] =
    for {
      events          <- parseBody[List[Event]](request.body)
      validatedEvents <- ZIO.collectAll(events.map(_.validateEvent))
      _               <- ZIO.collectAll(validatedEvents.map(Database.insertEvent))
    } yield Response.ok

  private def updateEvent(request: Request): ZIO[Database, DatabaseError, Response] =
    for {
      event          <- parseBody[UpdateEvent](request.body)
      validatedEvent <- event.validateUpdateEvent
      _              <- Database.updateEvent(validatedEvent)
    } yield Response.ok

  def apply(): App[Database] =
    Http
      .collectZIO[Request] {
        case Method.GET -> !! / "event"              => getEvent
        case request @ Method.POST -> !! / "collect" => insertEvent(request)
        case request @ Method.POST -> !! / "events"  => insertEvents(request)
        case request @ Method.POST -> !! / "update"  => updateEvent(request)
      }
      .mapError(_ => Response.ok)

}
