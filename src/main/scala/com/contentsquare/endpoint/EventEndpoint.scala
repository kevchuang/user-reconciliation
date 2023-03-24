package com.contentsquare.endpoint

import com.contentsquare.database.Database
import com.contentsquare.model.{Event, UpdateEvent}
import com.contentsquare.service.Parser
import com.contentsquare.service.Parser.Parser
import zio._
import zio.http._
import zio.http.model.Method
import zio.stream.{ZSink, ZStream}

object EventEndpoint {

  private[endpoint] def insertEventIntoDatabase(request: Request): ZIO[Database & Parser, Throwable, Response] =
    for {
      event          <- Parser.parseBody[Event](request.body)
      validatedEvent <- event.validateEvent
      _              <- Database.insertEvent(validatedEvent)
    } yield Response.ok

  private[endpoint] def insertEventSink(): ZSink[Database & Parser, Throwable, Request, Request, Response] =
    ZSink
      .take[Request](1)
      .map(_.headOption)
      .mapZIO {
        case Some(request) => insertEventIntoDatabase(request)
        case None          => ZIO.succeed(Response.ok)
      }

  private[endpoint] def updateEventInDatabase(request: Request): ZIO[Database & Parser, Throwable, Response] =
    for {
      event          <- Parser.parseBody[UpdateEvent](request.body)
      validatedEvent <- event.validateUpdateEvent
      _              <- Database.updateEvent(validatedEvent)
    } yield Response.ok

  private[endpoint] def updateEventSink(): ZSink[Database & Parser, Throwable, Request, Request, Response] =
    ZSink
      .take[Request](1)
      .map(_.headOption)
      .mapZIO {
        case Some(request) => updateEventInDatabase(request)
        case None          => ZIO.succeed(Response.ok)
      }

  def apply(): App[Database & Parser] = {
    Http
      .collectZIO[Request] {
        // POST /collect
        case request @ Method.POST -> !! / "collect" =>
          ZStream(request)
            .run(insertEventSink())
            .logError
        // POST /update
        case request @ Method.POST -> !! / "update"  =>
          ZStream(request)
            .schedule(Schedule.spaced(400.milliseconds))
            .run(updateEventSink())
            .logError
      }
      .mapError(_ => Response.ok)
  }
}
