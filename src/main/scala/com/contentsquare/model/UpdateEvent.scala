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

    /**
     * Returns an effect that will validate the input UpdateEvent and produces
     * an [[UpdateEvent]]. It may fail with [[EmptyValueException]] if userIds is empty
     * and fail with [[DataNotFoundException]] if the event doesn't exist in
     * Database.
     */
    def validateUpdateEvent: ZIO[Database, Throwable, UpdateEvent] = {
      for {
        _           <- ZIO
          .fail(EmptyValueException("At least one userId should be set in userIds field"))
          .unless(updateEvent.userIds.nonEmpty)
        existsEvent <- Database.existsEvent(updateEvent.id)
        _           <- ZIO.fail(DataNotFoundException("Event id doesn't exist")).unless(existsEvent)
      } yield updateEvent
    }
  }
}
