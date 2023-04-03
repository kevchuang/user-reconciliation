package com.contentsquare.service.buffer

import zio.Scope
import zio.test._
import zio.test.Assertion._

object RequestBufferSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    addSuite +
      removeSuite +
      hasReachedLimitSuite

  private lazy val addSuite =
    suite("add")(test("should add one to the buffer and return its count") {
      assertZIO(MetricsRequestBuffer.add)(equalTo(1))
    })
      .provide(MetricsRequestBuffer.layer)

  private lazy val removeSuite =
    suite("remove")(test("should remove one to the buffer and return its count") {
      assertZIO(MetricsRequestBuffer.remove)(equalTo(-1))
    })
      .provide(MetricsRequestBuffer.layer)

  private lazy val hasReachedLimitSuite =
    suite("hasReachedLimit")(
      test("should return true if buffer has reached its limit") {
        for {
          hasNotReachedLimitYet <- MetricsRequestBuffer.hasReachedLimit
          _                     <- MetricsRequestBuffer.add
          hasReachedLimit       <- MetricsRequestBuffer.hasReachedLimit
        } yield assertTrue(!hasNotReachedLimitYet, hasReachedLimit)
      }
    )
      .provide(MetricsRequestBuffer.layer)

}
