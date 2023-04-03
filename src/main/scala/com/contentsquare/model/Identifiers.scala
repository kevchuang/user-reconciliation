package com.contentsquare.model

import io.circe._
import zio.prelude._

import java.util.UUID

object Identifiers {
  object EventId extends Subtype[UUID]
  type EventId = EventId.Type

  object UserId extends Subtype[String]
  type UserId = UserId.Type

  object UserIdentifier extends Subtype[Set[UserId]]
  type UserIdentifier = UserIdentifier.Type

  implicit val eventIdDecoder: Decoder[EventId] = Decoder.decodeUUID.map(EventId(_))
  implicit val eventIdEncoder: Encoder[EventId] = (eventId: EventId) => Json.fromString(eventId.toString)

  implicit val userIdDecoder: Decoder[UserId] = Decoder.decodeString.map(UserId(_))
  implicit val userIdEncoder: Encoder[UserId] = (userId: UserId) => Json.fromString(userId)
}
