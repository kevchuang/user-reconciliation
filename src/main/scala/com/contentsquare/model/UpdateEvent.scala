package com.contentsquare.model

import com.contentsquare.database.Database
import com.contentsquare.error.DatabaseError.InvalidInput
import io.circe.{Decoder, Encoder}
import zio.ZIO
import io.circe.generic.semiauto._

import java.util.UUID

final case class UpdateEvent(
  id: UUID,
  userIds: Set[String]
)

object UpdateEvent {
  implicit val decoder: Decoder[UpdateEvent] = deriveDecoder[UpdateEvent]
  implicit val encoder: Encoder[UpdateEvent] = deriveEncoder[UpdateEvent]

  implicit class UpdateEventOps(updateEvent: UpdateEvent) {
    def validateUpdateEvent: ZIO[Database, InvalidInput, UpdateEvent] = {
      for {
        _ <-
          if (updateEvent.userIds.nonEmpty) ZIO.unit
          else ZIO.fail(InvalidInput("At least one userId should be set in userIds field"))
        _ <- Database
          .existsEvent(updateEvent.id)
          .flatMap(exists => if (exists) ZIO.unit else ZIO.fail(InvalidInput("Event id doesn't exist")))
      } yield updateEvent
    }
  }
}
