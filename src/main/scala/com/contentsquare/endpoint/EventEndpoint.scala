package com.contentsquare.endpoint

import com.contentsquare.database.Database
import com.contentsquare.error.Errors.DataNotFoundException
import com.contentsquare.model.{Event, UpdateEvent}
import com.contentsquare.service.buffer.EventRequestBuffer.EventRequestBuffer
import com.contentsquare.service.buffer.MetricsRequestBuffer.MetricsRequestBuffer
import com.contentsquare.service.buffer.{EventRequestBuffer, MetricsRequestBuffer}
import com.contentsquare.service.parser.Parser
import com.contentsquare.service.parser.Parser.Parser
import zio._
import zio.http._
import zio.http.model.Method

object EventEndpoint {

  /**
   * Returns an effect that will parse request body into [[Event]], validate
   * event data and insert it into Database, it produces a [[Response]] ok once
   * the insert is completed.
   */
  private[endpoint] def insertEventIntoDatabase(request: Request): ZIO[Database & Parser, Throwable, Response] =
    for {
      event          <- Parser.parseBody[Event](request.body)
      validatedEvent <- event.validateEvent
      _              <- Database.insertEvent(validatedEvent)
    } yield Response.ok

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
   * Takes as input an event request to process. It will first verify if event
   * request buffer or metrics request buffer has reached its limit. If one of
   * them has reached limit, it will pause the request for 50 milliseconds to
   * avoid overloading the server. It will then add one to event request buffer
   * and process the event request. Before, returning response it will remove
   * one from event request buffer.
   */
  private[endpoint] def runEventRequest(
    eventRequest: ZIO[Database & Parser, Throwable, Response]
  ): ZIO[Database & EventRequestBuffer & MetricsRequestBuffer & Parser, Throwable, Response] =
    for {
      hasReachedMetricsRequestLimit <- MetricsRequestBuffer.hasReachedLimit
      hasReachedEventRequestLimit   <- EventRequestBuffer.hasReachedLimit
      _        <- ZIO.sleep(50.milliseconds).unless(!hasReachedEventRequestLimit && !hasReachedMetricsRequestLimit)
      _        <- EventRequestBuffer.add
      response <- eventRequest
      _        <- EventRequestBuffer.remove
    } yield response

  /**
   * Creates a [[App]] app that catches POST /collect and POST /update requests.
   */
  def apply(): App[Database & Parser & EventRequestBuffer & MetricsRequestBuffer] = {
    Http
      .collectZIO[Request] {
        // POST /collect
        case request @ Method.POST -> !! / "collect" =>
          runEventRequest(insertEventIntoDatabase(request)).logError
        // POST /update
        case request @ Method.POST -> !! / "update"  =>
          runEventRequest(
            updateEventInDatabase(request).retryWhile {
              case _: DataNotFoundException => true
              case _                        => false
            }
          ).logError

      }
      .mapError(_ => Response.ok)
  }
}
