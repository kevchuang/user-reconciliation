package com.contentsquare.model

import com.contentsquare.error.DatabaseError.InvalidInput
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
    def validateEvent: ZIO[Any, InvalidInput, Event] =
      if (event.userIds.nonEmpty)
        ZIO.succeed(event)
      else
        ZIO.fail(InvalidInput("At least one userId should be set in userIds field"))
  }
}
