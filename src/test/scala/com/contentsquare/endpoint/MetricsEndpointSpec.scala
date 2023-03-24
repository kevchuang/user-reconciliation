package com.contentsquare.endpoint

import com.contentsquare.database.{Database, InMemoryDatabase}
import com.contentsquare.model.{EventType, Metrics, Source, User}
import com.contentsquare.utils.DataGenerator
import io.circe.syntax._
import zio.Scope
import zio.http.Response
import zio.test.Assertion._
import zio.test._

import java.util.UUID

object MetricsEndpointSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    countBouncedUsersSuite + countCrossDeviceUsersSuite + getMetricsSuite

  private lazy val countBouncedUsersSuite =
    suite("countBouncedUsers")(
      test("should return total of users that have only one display event") {
        val eventDisplay = DataGenerator.generateRandomEvent(event = EventType.display)
        val eventBuy     = DataGenerator.generateRandomEvent(event = EventType.buy)

        val users = List(
          DataGenerator.generateRandomUser(events = Set(eventDisplay)),
          DataGenerator.generateRandomUser(events = Set(eventBuy)),
          DataGenerator.generateRandomUser(events = Set(eventDisplay, eventBuy)),
          DataGenerator.generateRandomUser(events =
            Set(eventDisplay, DataGenerator.generateRandomEvent(event = EventType.display))
          )
        )

        assertZIO(MetricsEndpoint.countBouncedUsers(users))(equalTo(1L))
      },
      test("should return zero if list of User is empty") {
        assertZIO(MetricsEndpoint.countBouncedUsers(List.empty[User]))(equalTo(0L))
      }
    )

  private lazy val countCrossDeviceUsersSuite =
    suite("countCrossDeviceUsers")(
      test("should return total of users that have both sources (appscreen and webpage)") {
        val userAppScreen = DataGenerator.generateRandomUser(sources = Set(Source.appscreen))
        val userWebpage = DataGenerator.generateRandomUser(sources = Set(Source.webpage))
        val userBoth = DataGenerator.generateRandomUser(sources = Set(Source.appscreen, Source.webpage))
        val users = List(
          userAppScreen,
          userWebpage,
          userBoth
        )

        assertZIO(MetricsEndpoint.countCrossDeviceUsers(users))(equalTo(1L))
      },
      test("should return zero if list of User is empty") {
        assertZIO(MetricsEndpoint.countCrossDeviceUsers(List.empty[User]))(equalTo(0L))
      }
    )

  private lazy val getMetricsSuite =
    suite("getMetrics")(
      test("should return a Metrics in json format in Response object") {
        val firstUserEventDisplay = DataGenerator.generateRandomEvent(source = Source.appscreen, event = EventType.display)
        val firstUserEventBuy = DataGenerator.generateRandomEvent(userIds = firstUserEventDisplay.userIds + UUID.randomUUID.toString, source = Source.webpage, event = EventType.buy)
        val secondUserEventDisplay     = DataGenerator.generateRandomEvent(source = Source.webpage, event = EventType.display)
        val expected = Response.json(
          Metrics(
            uniqueUsers = 2,
            bouncedUsers = 1,
            crossDeviceUsers = 1
          ).asJson.noSpaces
        )

        for {
          _ <- Database.insertEvent(firstUserEventDisplay)
          _ <- Database.insertEvent(firstUserEventBuy)
          _ <- Database.insertEvent(secondUserEventDisplay)
          response <- MetricsEndpoint.getMetrics
        } yield assert(response)(equalTo(expected))
      }
    ).provide(InMemoryDatabase.layer)
}
