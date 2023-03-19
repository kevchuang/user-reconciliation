package com.contentsquare.database

import com.contentsquare.error.DatabaseError
import com.contentsquare.error.DatabaseError.InvalidInput
import com.contentsquare.model.{Event, UpdateEvent}
import zio.{Task, UIO, ZIO}

import java.util.UUID

trait Database {
  def exists(id: UUID): UIO[Boolean]

  def getAll: Task[List[Event]]

  def get(id: UUID): Task[Option[Event]]

  def put(event: Event): Task[Option[Event]]

  def update(event: UpdateEvent): Task[Unit]
}

object Database {
  def exists(id: UUID): ZIO[Database, Nothing, Boolean] =
    ZIO.serviceWithZIO[Database](_.exists(id))

  def getAll: ZIO[Database, Throwable, List[Event]] =
    ZIO.serviceWithZIO[Database](_.getAll)

  def get(id: UUID): ZIO[Database, Throwable, Option[Event]] =
    ZIO.serviceWithZIO[Database](_.get(id))

  def put(event: Event): ZIO[Database, DatabaseError, Option[Event]] =
    ZIO
      .serviceWithZIO[Database](_.put(event))
      .mapError(e => InvalidInput(e.getMessage))

  def update(event: UpdateEvent): ZIO[Database, DatabaseError, Unit] =
    ZIO
      .serviceWithZIO[Database](_.update(event))
      .mapError(e => InvalidInput(e.getMessage))
}
