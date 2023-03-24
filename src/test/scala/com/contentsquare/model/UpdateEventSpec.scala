package com.contentsquare.model

import com.contentsquare.database.{Database, InMemoryDatabase}
import com.contentsquare.error.Errors.{DataNotFoundException, EmptyValueException}
import com.contentsquare.utils.DataGenerator
import zio.Scope
import zio.test._
import zio.test.Assertion._

object UpdateEventSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    updateEventSuite

  private lazy val updateEventSuite =
    suite("validateUpdateEvent")(
      test("should validate an UpdateEvent and return it") {
        val event       = DataGenerator.generateRandomEvent()
        val updateEvent = DataGenerator.generateRandomUpdateEvent(id = event.id)

        for {
          _                     <- Database.insertEvent(event)
          validatedUpdatedEvent <- updateEvent.validateUpdateEvent
        } yield assert(validatedUpdatedEvent)(equalTo(updateEvent))
      },
      test("should fail with EmptyValueException if userIds is not set") {
        val updateEvent = DataGenerator.generateRandomUpdateEvent(userIds = Set.empty[String])

        assertZIO(updateEvent.validateUpdateEvent.exit)(fails(isSubtype[EmptyValueException](anything)))
      },
      test("should fail with DataNotFoundException if event id to update is not in database") {
        val updateEvent = DataGenerator.generateRandomUpdateEvent()

        assertZIO(updateEvent.validateUpdateEvent.exit)(fails(isSubtype[DataNotFoundException](anything)))
      }
    ).provide(InMemoryDatabase.layer)
}
