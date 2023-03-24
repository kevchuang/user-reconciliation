package com.contentsquare.model

import com.contentsquare.error.Errors.EmptyValueException
import com.contentsquare.model.EventType.EventType
import com.contentsquare.model.Source.Source
import io.circe._
import io.circe.generic.semiauto._
import zio.ZIO

import java.util.UUID

final case class Event(
  id: UUID,
  source: Source,
  event: EventType,
  userIds: Set[String]
)

object Event {
  implicit val decoder: Decoder[Event] = deriveDecoder[Event]
  implicit val encoder: Encoder[Event] = deriveEncoder[Event]

  implicit class EventOps(event: Event) {

    /**
     * Returns an effect that will validate the input [[Event]] and produces an
     * [[Event]]. It may fail with [[EmptyValueException]] if userIds is empty.
     */
    def validateEvent: ZIO[Any, Throwable, Event] =
      if (event.userIds.nonEmpty)
        ZIO.succeed(event)
      else
        ZIO.fail(EmptyValueException("At least one userId should be set in userIds field"))

  }
}
