package com.contentsquare.endpoint

import com.contentsquare.database.{Database, InMemoryDatabase}
import com.contentsquare.error.Errors.{DataNotFoundException, InvalidInputDataException}
import com.contentsquare.service.buffer.{EventRequestBuffer, MetricsRequestBuffer}
import com.contentsquare.service.parser.Parser
import com.contentsquare.utils.DataGenerator
import io.circe.syntax._
import zio.http.model.Method
import zio.http.{Body, Request, Response, URL}
import zio.test.Assertion._
import zio.test._
import zio.{Scope, ZIO, durationInt}

object EventEndpointSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    insertEventIntoDatabaseSuite + updateEventInDatabaseSuite + runEventRequestSuite

  private lazy val insertEventIntoDatabaseSuite =
    suite("insertEventIntoDatabase")(
      test("should insert an Event and respond ok") {
        val event   = DataGenerator.generateRandomEvent()
        val request = Request.default(
          method = Method.POST,
          url = URL.empty,
          body = Body.fromString(event.asJson.noSpaces)
        )

        for {
          response      <- EventEndpoint.insertEventIntoDatabase(request)
          eventInserted <- Database.existsEvent(event.id)
        } yield assertTrue(eventInserted) &&
          assert(response)(equalTo(Response.ok))
      },
      test("should fail with InvalidDataException if body is empty") {
        val request = Request.default(Method.POST, URL.empty)

        assertZIO(EventEndpoint.insertEventIntoDatabase(request).exit)(
          fails(isSubtype[InvalidInputDataException](anything))
        )
      }
    ).provide(InMemoryDatabase.layer, Parser.layer)

  private lazy val updateEventInDatabaseSuite =
    suite("updateEventInDatabase")(
      test("should update an Event and respond ok") {
        val event               = DataGenerator.generateRandomEvent()
        val updateEvent         = DataGenerator.generateRandomUpdateEvent(id = event.id)
        val request             = Request.default(
          method = Method.POST,
          url = URL.empty,
          body = Body.fromString(updateEvent.asJson.noSpaces)
        )
        val updatedEvent        = event.copy(userIds = updateEvent.userIds)
        val expectedUpdatedUser = DataGenerator.generateUserFromEvent(updatedEvent)

        for {
          _                    <- Database.insertEvent(event)
          initialEventInserted <- Database.existsEvent(event.id)
          response             <- EventEndpoint.updateEventInDatabase(request)
          users                <- Database.getUsers
        } yield assertTrue(initialEventInserted) &&
          assert(users)(hasSameElements(List(expectedUpdatedUser))) &&
          assert(response)(equalTo(Response.ok))
      },
      test("should fail with DataNotFoundException if event id doesn't exist") {
        val updateEvent = DataGenerator.generateRandomUpdateEvent()
        val request     = Request.default(
          method = Method.POST,
          url = URL.empty,
          body = Body.fromString(updateEvent.asJson.noSpaces)
        )

        assertZIO(EventEndpoint.updateEventInDatabase(request).exit)(
          fails(isSubtype[DataNotFoundException](anything))
        )
      },
      test("should fail with InvalidDataException if body is empty") {
        val request = Request.default(Method.POST, URL.empty)

        assertZIO(EventEndpoint.updateEventInDatabase(request).exit)(
          fails(isSubtype[InvalidInputDataException](anything))
        )
      }
    ).provide(InMemoryDatabase.layer, Parser.layer)

  private val runEventRequestSuite =
    suite("runEventRequest")(
      test("should run event request without sleeping and return its response") {
        val event   = DataGenerator.generateRandomEvent()
        val request = Request.default(
          method = Method.POST,
          url = URL.empty,
          body = Body.fromString(event.asJson.noSpaces)
        )

        for {
          response      <- EventEndpoint.runEventRequest(EventEndpoint.insertEventIntoDatabase(request))
          eventInserted <- Database.existsEvent(event.id)
        } yield assertTrue(eventInserted) &&
          assert(response)(equalTo(Response.ok))
      }
        .provide(InMemoryDatabase.layer, EventRequestBuffer.layer, MetricsRequestBuffer.layer, Parser.layer),
      test("should sleep 50 milliseconds before run event request") {
        val event   = DataGenerator.generateRandomEvent()
        val request = Request.default(
          method = Method.POST,
          url = URL.empty,
          body = Body.fromString(event.asJson.noSpaces)
        )

        for {
          start         <- ZIO.succeed(System.currentTimeMillis)
          _             <- MetricsRequestBuffer.add
          responseFiber <- EventEndpoint.runEventRequest(EventEndpoint.insertEventIntoDatabase(request)).fork
          _             <- TestClock.adjust(50.milliseconds)
          response      <- responseFiber.join
          eventInserted <- Database.existsEvent(event.id)
          end           <- ZIO.succeed(System.currentTimeMillis)
        } yield assertTrue(eventInserted, end - start >= 50) &&
          assert(response)(equalTo(Response.ok))
      }
        .provide(InMemoryDatabase.layer, EventRequestBuffer.layer, MetricsRequestBuffer.layer, Parser.layer)
    )
}
