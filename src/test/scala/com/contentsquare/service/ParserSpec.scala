package com.contentsquare.service

import com.contentsquare.error.Errors.InvalidInputDataException
import com.contentsquare.model.Event
import com.contentsquare.utils.DataGenerator
import io.circe.syntax._
import zio.Scope
import zio.http.Body
import zio.test.Assertion._
import zio.test._

object ParserSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    parseBodySuite

  private lazy val parseBodySuite =
    suite("parseBody")(
      test("should return an Event if Body is containing an Event in json format") {
        val event = DataGenerator.generateRandomEvent()
        val body  = Body.fromString(event.asJson.noSpaces)

        assertZIO(Parser.parseBody[Event](body))(equalTo(event))
      },
      test("should fail with InvalidInputData if Body is incorrect") {
        val body = Body.fromString("abc")

        assertZIO(Parser.parseBody[Event](body).exit)(fails(isSubtype[InvalidInputDataException](anything)))
      }
    ).provide(Parser.layer)
}
