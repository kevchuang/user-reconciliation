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

object Source extends Enumeration {
  type Source = Value
  val webpage, appscreen = Value
}

object EventType extends Enumeration {
  type EventType = Value
  val display, buy = Value
}

object Event {
  implicit val eventTypeDecoder: Decoder[EventType.Value] =
    Decoder.decodeEnumeration(EventType)
  implicit val eventTypeEncoder: Encoder[EventType.Value] =
    Encoder.encodeEnumeration(EventType)
  implicit val sourceDecoder: Decoder[Source.Value]       =
    Decoder.decodeEnumeration(Source)
  implicit val sourceEncoder: Encoder[Source.Value]       =
    Encoder.encodeEnumeration(Source)

  implicit val eventDecoder: Decoder[Event] = deriveDecoder[Event]
  implicit val eventEncoder: Encoder[Event] = deriveEncoder[Event]

  implicit class EventOps(event: Event) {
    def validateEvent: ZIO[Any, InvalidInput, Event] =
      if (event.userIds.nonEmpty)
        ZIO.succeed(event)
      else
        ZIO.fail(InvalidInput("At least one userId should be set in userIds field"))
  }
}
