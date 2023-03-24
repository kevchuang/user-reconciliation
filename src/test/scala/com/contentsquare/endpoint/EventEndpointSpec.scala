package com.contentsquare.endpoint

import com.contentsquare.database.{Database, InMemoryDatabase}
import com.contentsquare.error.Errors.{DataNotFoundException, InvalidInputDataException}
import com.contentsquare.service.Parser
import com.contentsquare.utils.DataGenerator
import io.circe.syntax._
import zio.Scope
import zio.http.model.Method
import zio.http.{Body, Request, Response, URL}
import zio.stream.ZStream
import zio.test.Assertion._
import zio.test._

object EventEndpointSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    insertEventIntoDatabaseSuite + insertEventSinkSuite + updateEventInDatabaseSuite + updateEventSinkSuite

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

  private lazy val insertEventSinkSuite =
    suite("insertEventSinkSuite")(
      test("should take a request, execute insertEventIntoDatabase and return a Response ok") {
        val event   = DataGenerator.generateRandomEvent()
        val request = Request.default(
          method = Method.POST,
          url = URL.empty,
          body = Body.fromString(event.asJson.noSpaces)
        )

        for {
          response      <- ZStream.succeed(request).run(EventEndpoint.insertEventSink())
          eventInserted <- Database.existsEvent(event.id)
        } yield assertTrue(eventInserted) &&
          assert(response)(equalTo(Response.ok))
      },
      test("should fail with InvalidDataException if body is empty") {
        val request = Request.default(Method.POST, URL.empty)

        assertZIO(ZStream.succeed(request).run(EventEndpoint.insertEventSink()).exit)(
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

  private lazy val updateEventSinkSuite =
    suite("updateEventSink")(
      test("should take a request, execute updateEventInDatabase and return a Response ok") {
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
          response             <- ZStream.succeed(request).run(EventEndpoint.updateEventSink())
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

        assertZIO(ZStream.succeed(request).run(EventEndpoint.updateEventSink()).exit)(
          fails(isSubtype[DataNotFoundException](anything))
        )
      },
      test("should fail with InvalidDataException if body is empty") {
        val request = Request.default(Method.POST, URL.empty)

        assertZIO(ZStream.succeed(request).run(EventEndpoint.updateEventSink()).exit)(
          fails(isSubtype[InvalidInputDataException](anything))
        )
      }
    ).provide(InMemoryDatabase.layer, Parser.layer)
}
