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
import zio.http.model.Method
import zio.stream.{ZSink, ZStream}

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

  private def insertEvent(request: Request): ZIO[Database, DatabaseError, Response] =
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

  private def updateEvent(request: Request): ZIO[Database, DatabaseError, Response] =
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

//  private def insertEventStream(request: Request): ZIO[Database, DatabaseError, Response] =
//    for {
//      event          <- parseBody[Event](request.body)
//      validatedEvent <- event.validateEvent
//      _              <- Database.insertEventStream(validatedEvent)
//    } yield Response.ok

  val queue: UIO[Queue[Request]] = Queue.bounded[Request](1000)

  def apply(): App[Database] = {
    val sink: ZSink[Database, DatabaseError, Request, Request, Response] =
      ZSink
        .take[Request](1)
        .map(_.headOption.getOrElse(throw new Exception("Error")))
        .mapZIO {
          case request @ Method.POST -> !! / "collect" => insertEvent(request)
          case request @ Method.POST -> !! / "update"  => updateEvent(request)
          case _                                       => ZIO.succeed(Response.ok)
        }

    Http
      .collectZIO[Request] {
        case request: Request if request.method == Method.POST =>
        for {
          q        <- queue
          _        <- q.offer(request)
          response <- ZStream.fromQueue(q).schedule(Schedule.spaced(10.microseconds)).run(sink)
        } yield response
      }
      .mapError(_ => Response.ok)
  }
}
