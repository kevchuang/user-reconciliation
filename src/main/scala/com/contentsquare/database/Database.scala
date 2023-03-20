package com.contentsquare.database

import com.contentsquare.error.DatabaseError
import com.contentsquare.error.DatabaseError.InvalidInput
import com.contentsquare.model.{Event, UpdateEvent, UserEvent}
import zio.{Task, UIO, ZIO}

import java.util.UUID

trait Database {
  def existsEvent(id: UUID): UIO[Boolean]

  def getUserEvents: UIO[List[UserEvent]]

  def insertEvent(event: Event): UIO[Option[UserEvent]]

  def updateEvent(updateEvent: UpdateEvent): Task[Option[UserEvent]]
}

object Database {
  def existsEvent(id: UUID): ZIO[Database, Nothing, Boolean] =
    ZIO.serviceWithZIO[Database](_.existsEvent(id))

  def getUserEvents: ZIO[Database, Nothing, List[UserEvent]] =
    ZIO.serviceWithZIO[Database](_.getUserEvents)

  def insertEvent(event: Event): ZIO[Database, Nothing, Option[UserEvent]] =
    ZIO.serviceWithZIO[Database](_.insertEvent(event))

  def updateEvent(event: UpdateEvent): ZIO[Database, DatabaseError, Option[UserEvent]] =
    ZIO
      .serviceWithZIO[Database](_.updateEvent(event))
      .mapError(e => InvalidInput(e.getMessage))
}
