package com.contentsquare.database

import com.contentsquare.error.DatabaseError
import com.contentsquare.error.DatabaseError.InvalidInput
import com.contentsquare.model.{Event, UpdateEvent, User}
import zio.{Task, UIO, ZIO}

import java.util.UUID

trait Database {
  def existsEvent(id: UUID): UIO[Boolean]

  def getUsers: UIO[List[User]]

  def insertEvent(event: Event): UIO[Option[User]]

  def updateEvent(updateEvent: UpdateEvent): Task[List[User]]
}

object Database {
  def existsEvent(id: UUID): ZIO[Database, Nothing, Boolean] =
    ZIO.serviceWithZIO[Database](_.existsEvent(id))

  def getUsers: ZIO[Database, Nothing, List[User]] =
    ZIO.serviceWithZIO[Database](_.getUsers)

  def insertEvent(event: Event): ZIO[Database, Nothing, Option[User]] =
    ZIO.serviceWithZIO[Database](_.insertEvent(event))

  def updateEvent(event: UpdateEvent): ZIO[Database, DatabaseError, List[User]] =
    ZIO
      .serviceWithZIO[Database](_.updateEvent(event))
      .mapError(e => InvalidInput(e.getMessage))
}
