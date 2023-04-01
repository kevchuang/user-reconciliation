package com.contentsquare.database

import com.contentsquare.model.Identifiers.{EventId, Id, UserIdentifier}
import com.contentsquare.model.{Event, UpdateEvent, User}
import zio.{Task, UIO, ZIO}

//import java.util.UUID

trait Database {
//  def existsEventWithUUID(id: UUID): UIO[Boolean]

  def existsEvent(id: EventId): UIO[Boolean]

  def getLinks: UIO[Map[Id, UserIdentifier]]

  def getUniqueUsers: UIO[List[User]]

  def insertEvent(event: Event): UIO[Unit]

  def updateEvent(updateEvent: UpdateEvent): Task[Unit]
}

object Database {
//  def existsEventWithUUID(id: UUID): ZIO[Database, Nothing, Boolean] =
//    ZIO.serviceWithZIO[Database](_.existsEventWithUUID(id))

  def existsEvent(id: EventId): ZIO[Database, Nothing, Boolean] =
    ZIO.serviceWithZIO[Database](_.existsEvent(id))

  def getLinks: ZIO[Database, Nothing, Map[Id, UserIdentifier]] =
    ZIO.serviceWithZIO[Database](_.getLinks)

  def getUniqueUsers: ZIO[Database, Nothing, List[User]] =
    ZIO.serviceWithZIO[Database](_.getUniqueUsers)

  def insertEvent(event: Event): ZIO[Database, Nothing, Unit] =
    ZIO.serviceWithZIO[Database](_.insertEvent(event))

  def updateEvent(event: UpdateEvent): ZIO[Database, Throwable, Unit] =
    ZIO.serviceWithZIO[Database](_.updateEvent(event))
}
