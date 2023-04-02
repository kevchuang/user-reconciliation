package com.contentsquare.endpoint

import com.contentsquare.database.Database
import com.contentsquare.error.Errors.DataNotFoundException
import com.contentsquare.model.{Event, UpdateEvent}
import com.contentsquare.request.RequestBuffer
import com.contentsquare.request.RequestBuffer.RequestBuffer
import com.contentsquare.service.Parser
import com.contentsquare.service.Parser.Parser
import zio._
import zio.http._
import zio.http.model.Method
import zio.stream.ZSink

object EventEndpoint {

  /**
   * Returns an effect that will parse request body into [[Event]], validate
   * event data and insert it into Database, it produces a [[Response]] ok once
   * the insert is completed.
   */
  def insertEventIntoDatabase(request: Request): ZIO[Database & Parser, Throwable, Response] =
    for {
      event          <- Parser.parseBody[Event](request.body)
      validatedEvent <- event.validateEvent
      _              <- Database.insertEvent(validatedEvent)
    } yield Response.ok

  /**
   * Sink that takes a [[Request]] and call [[insertEventIntoDatabase]] and
   * return its [[Response]].
   */
  private[endpoint] def insertEventSink(): ZSink[Database & Parser, Throwable, Request, Request, Response] =
    ZSink
      .take[Request](1)
      .map(_.headOption)
      .mapZIO {
        case Some(request) => insertEventIntoDatabase(request)
        case None          => ZIO.succeed(Response.ok)
      }

  /**
   * Returns an effect that will parse request body into UpdateEvent, validate
   * update event data and update the corresponding event in Database, it
   * produces a [[Response]] ok once the insert is completed.
   */
  private[endpoint] def updateEventInDatabase(request: Request): ZIO[Database & Parser, Throwable, Response] =
    for {
      event          <- Parser.parseBody[UpdateEvent](request.body)
      validatedEvent <- event.validateUpdateEvent
      _              <- Database.updateEvent(validatedEvent)
    } yield Response.ok

  /**
   * Sink that takes a [[Request]] and call [[updateEventInDatabase]] and return
   * its Response.
   */
  private[endpoint] def updateEventSink(): ZSink[Database & Parser, Throwable, Request, Request, Response] =
    ZSink
      .take[Request](1)
      .map(_.headOption)
      .mapZIO {
        case Some(request) => updateEventInDatabase(request)
        case None          => ZIO.succeed(Response.ok)
      }

  /**
   * Creates a [[App]] app that catches POST /collect and POST /update requests.
   */
  def apply(): App[Database & Parser & RequestBuffer] = {
    Http
      .collectZIO[Request] {
        // POST /collect
        case request @ Method.POST -> !! / "collect" =>
          (for {
            hasReachedMetricsRequestLimit <- RequestBuffer.hasReachedMetricsRequestLimit
            hasReachedEventRequestLimit   <- RequestBuffer.hasReachedEventRequestLimit
            _        <- ZIO.sleep(1.milliseconds).unless(!hasReachedEventRequestLimit)
            _        <- ZIO.sleep(100.milliseconds).unless(!hasReachedMetricsRequestLimit)
            _        <- RequestBuffer.addEventRequestIntoBuffer()
            response <- insertEventIntoDatabase(request)
            _        <- RequestBuffer.removeEventRequestFromBuffer()
          } yield response).logError
        // POST /update  - Scheduling update request to make sure that event is inserted first when receiving 1000 requests per second
        case request @ Method.POST -> !! / "update"  =>
          (for {
            hasReachedMetricsRequestLimit <- RequestBuffer.hasReachedMetricsRequestLimit
            hasReachedEventRequestLimit   <- RequestBuffer.hasReachedEventRequestLimit
            _        <- ZIO.sleep(20.milliseconds).unless(!hasReachedMetricsRequestLimit && !hasReachedEventRequestLimit)
            _        <- RequestBuffer.addEventRequestIntoBuffer()
            response <- updateEventInDatabase(request).retryWhile {
              case _: DataNotFoundException => true
              case _                        => false
            }
            _        <- RequestBuffer.removeEventRequestFromBuffer()
          } yield response).logError

      }
      .mapError(_ => Response.ok)
  }
}
