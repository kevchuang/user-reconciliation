package com.contentsquare.database

import com.contentsquare.model.{Event, UpdateEvent, User}
import zio.{Task, UIO, ZIO}

import java.util.UUID

trait Database {
  def existsEvent(id: UUID): UIO[Boolean]

  def getUsers: UIO[List[User]]

  def insertEvent(event: Event): UIO[Unit]

  def updateEvent(updateEvent: UpdateEvent): Task[Unit]
}

object Database {
  def existsEvent(id: UUID): ZIO[Database, Nothing, Boolean] =
    ZIO.serviceWithZIO[Database](_.existsEvent(id))

  def getUsers: ZIO[Database, Nothing, List[User]] =
    ZIO.serviceWithZIO[Database](_.getUsers)

  def insertEvent(event: Event): ZIO[Database, Nothing, Unit] =
    ZIO.serviceWithZIO[Database](_.insertEvent(event))

  def updateEvent(event: UpdateEvent): ZIO[Database, Throwable, Unit] =
    ZIO.serviceWithZIO[Database](_.updateEvent(event))
}
