package com.contentsquare.model

import com.contentsquare.error.Errors.EmptyValueException
import com.contentsquare.utils.DataGenerator
import zio.Scope
import zio.test.Assertion._
import zio.test._

object EventSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    validateEventSuite

  private lazy val validateEventSuite =
    suite("validateEvent")(
      test("should validate Event and return it") {
        val event = DataGenerator.generateRandomEvent()

        assertZIO(event.validateEvent)(equalTo(event))
      },
      test("should fail with EmptyValueException if usersIds is empty") {
        val event = DataGenerator.generateRandomEvent(userIds = Set.empty[String])

        assertZIO(event.validateEvent.exit)(fails(isSubtype[EmptyValueException](anything)))
      }
    )
}
