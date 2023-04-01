package com.contentsquare.model

import io.circe._
import zio.prelude.Subtype

import java.util.UUID

object Identifiers {
  sealed trait Id extends Product with Serializable

  final case class EventId(value: UUID) extends Id
  final case class UserId(value: String) extends Id

  object UserIdentifier extends Subtype[UUID]
  type UserIdentifier = UserIdentifier.Type

  implicit val eventIdDecoder: Decoder[EventId] = Decoder.decodeUUID.map(uuid => EventId(uuid))
  implicit val eventIdEncoder: Encoder[EventId] = (eventId: EventId) => Json.fromString(eventId.value.toString)

  implicit val userIdDecoder: Decoder[UserId] = Decoder.decodeString.map(userId => UserId(userId))
  implicit val userIdEncoder: Encoder[UserId] = (userId: UserId) => Json.fromString(userId.value)

  implicit val userIdentifierDecoder: Decoder[UserIdentifier] = Decoder.decodeUUID.map(UserIdentifier(_))
  implicit val userIdentifierEncoder: Encoder[UserIdentifier] = (userIdentifier: UserIdentifier) =>
    Json.fromString(userIdentifier.toString)

}
