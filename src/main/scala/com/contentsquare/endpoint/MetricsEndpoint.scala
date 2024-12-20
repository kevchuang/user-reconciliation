package com.contentsquare.endpoint

import com.contentsquare.database.Database
import com.contentsquare.model.{EventType, Metrics, User}
import com.contentsquare.service.buffer.MetricsRequestBuffer
import com.contentsquare.service.buffer.MetricsRequestBuffer.MetricsRequestBuffer
import io.circe.syntax._
import zio._
import zio.http._
import zio.http.model.Method

object MetricsEndpoint {

  /**
   * Returns an effect that will count users given as input that contain only
   * one display event.
   */
  private[endpoint] def countBouncedUsers(users: List[User]): UIO[Long] =
    ZIO.succeed(
      users.count(user => user.events.size == 1 && user.events.exists(_.event == EventType.display)).toLong
    )

  /**
   * Returns an effect that will count users given as input that contain both
   * appscreen and webpage sources.
   */
  private[endpoint] def countCrossDeviceUsers(users: List[User]): UIO[Long] =
    ZIO.succeed(
      users.count(_.sources.size == 2).toLong
    )

  /**
   * Returns an effect that will generate a Metrics and count unique users,
   * bounded users and cross device users and produce an [[Response]] that
   * contains a body of [[Metrics]] in json format.
   */
  private[endpoint] def getMetrics: ZIO[Database, Throwable, Response] =
    for {
      users            <- Database.getUsers
      bouncedUsers     <- countBouncedUsers(users)
      crossDeviceUsers <- countCrossDeviceUsers(users)
    } yield Response.json(
      Metrics(
        uniqueUsers = users.length.toLong,
        bouncedUsers = bouncedUsers,
        crossDeviceUsers = crossDeviceUsers
      ).asJson.noSpaces
    )

  /**
   * Returns a [[App]] that catches GET /metrics request and produces a
   * [[Response]] containing [[Metrics]] in json format.
   */
  def apply(): App[Database & MetricsRequestBuffer] =
    Http
      .collectZIO[Request] {
        // GET /metrics
        case Method.GET -> !! / "metrics" =>
          for {
            _        <- MetricsRequestBuffer.add
            response <- getMetrics
            _        <- MetricsRequestBuffer.remove
          } yield response
      }
      .withDefaultErrorResponse
}
