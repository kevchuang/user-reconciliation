package com.contentsquare.endpoint

import com.contentsquare.database.Database
import com.contentsquare.model.{EventType, Metrics, User}
import zio._
import zio.http.model.Method
import zio.http._
import io.circe.syntax._

object MetricsEndpoint {

  private[endpoint] def countBouncedUsers(users: List[User]): UIO[Long] =
    ZIO.succeed(
      users.count(user => user.events.size == 1 && user.events.exists(_.event == EventType.display)).toLong
    )

  private[endpoint] def countCrossDeviceUsers(users: List[User]): UIO[Long] =
    ZIO.succeed(
      users.count(_.sources.size == 2).toLong
    )

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

  def apply(): App[Database] =
    Http
      .collectZIO[Request] {
        // GET /metrics
        case Method.GET -> !! / "metrics" => getMetrics
      }
      .withDefaultErrorResponse
}
