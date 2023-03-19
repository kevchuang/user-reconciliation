package com.contentsquare.database

import com.contentsquare.model.{Event, UpdateEvent}
import zio.{Task, UIO, ZIO, ZLayer}

import java.util.UUID
import scala.collection.mutable

final case class InMemoryDatabase(database: mutable.Map[UUID, Event]) extends Database {
  override def exists(id: UUID): UIO[Boolean] = ZIO.succeed(database.exists(id == _._1))

  override def getAll: Task[List[Event]] = ZIO.attempt(database.values.toList)

  override def get(id: UUID): Task[Option[Event]] =
    ZIO.attempt(database.get(id))

  override def put(event: Event): Task[Option[Event]] =
    ZIO.attempt(database.put(event.id, event))

  override def update(event: UpdateEvent): Task[Unit] =
    ZIO.attempt(
      for {
        eventToUpdate <- database.get(event.id)
      } yield database.update(
        key = event.id,
        value = eventToUpdate.copy(userIds = event.userIds)
      )
    )
}

object InMemoryDatabase {

  val layer: ZLayer[Any, Nothing, InMemoryDatabase] =
    ZLayer.succeed(InMemoryDatabase(mutable.Map.empty[UUID, Event]))

}
