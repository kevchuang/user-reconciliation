package com.contentsquare.endpoint

import com.contentsquare.database.Database
import com.contentsquare.model.{EventType, Metrics, UserEvent}
import zio.ZIO
import zio.http.model.Method
import zio.http._
import io.circe.syntax._

object MetricsEndpoint {

  private[endpoint] def computeMetric(
    userEvent: List[UserEvent],
    computation: List[UserEvent] => Long
  ): ZIO[Any, Nothing, Long] = {
    ZIO.succeed(computation(userEvent))
  }

  private[endpoint] def getMetrics: ZIO[Database, Throwable, Response] =
    for {
      userEvents       <- Database.getUserEvents
      uniqueUsers      <- ZIO.succeed(userEvents.length)
      bouncedUsers     <- ZIO.succeed(
        userEvents.foldLeft(0L)((acc, userEvent) =>
          if (userEvent.events.size == 1 && userEvent.events.count(_._2.event == EventType.display) == 1)
            acc + 1
          else
            acc
        )
      )
      crossDeviceUsers <- ZIO.succeed(
        userEvents.foldLeft(0L)((acc, userEvent) =>
          if (userEvent.sources.size == 2)
            acc + 1
          else
            acc
        )
      )
    } yield Response.json(
      Metrics(
        uniqueUsers = uniqueUsers.toLong,
        bouncedUsers = bouncedUsers,
        crossDeviceUsers = crossDeviceUsers
      ).asJson.noSpaces
    )

  def apply(): App[Database] =
    Http
      .collectZIO[Request] {
        // GET /metrics
        case Method.GET -> !! / "metrics" => getMetrics
      }
      .withDefaultErrorResponse
}
