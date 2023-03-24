package com.contentsquare.model

import com.contentsquare.database.Database
import com.contentsquare.error.Errors.{DataNotFoundException, EmptyValueException}
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import zio.ZIO

import java.util.UUID

final case class UpdateEvent(
  id: UUID,
  userIds: Set[String]
)

object UpdateEvent {
  implicit val decoder: Decoder[UpdateEvent] = deriveDecoder[UpdateEvent]
  implicit val encoder: Encoder[UpdateEvent] = deriveEncoder[UpdateEvent]

  implicit class UpdateEventOps(updateEvent: UpdateEvent) {
    def validateUpdateEvent: ZIO[Database, Throwable, UpdateEvent] = {
      for {
        _ <-
          if (updateEvent.userIds.nonEmpty)
            ZIO.unit
          else
            ZIO.fail(EmptyValueException("At least one userId should be set in userIds field"))
        existsEvent <- Database.existsEvent(updateEvent.id)
          _ <- if (existsEvent) ZIO.unit else ZIO.fail(DataNotFoundException("Event id doesn't exist"))
      } yield updateEvent
    }
  }
}
