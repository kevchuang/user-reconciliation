package com.contentsquare.database
import com.contentsquare.model.{Event, UpdateEvent, UserEvent}
import zio.{Task, UIO, ZIO, ZLayer}

import java.util.UUID
import scala.collection.mutable

final case class InMemoryDatabase(database: mutable.HashMap[Set[String], UserEvent]) extends Database {
  def existsEvent(id: UUID): UIO[Boolean] =
    ZIO.succeed(database.exists(userEvent => userEvent._2.events.contains(id)))

  def getUserEvents: UIO[List[UserEvent]] =
    ZIO.succeed(database.values.toList)

  def getLinkedUserEvents(userIds: Set[String]): ZIO[Any, Nothing, Map[Set[String], UserEvent]] =
    ZIO.succeed(
      database
        .filter(userEvent => userIds.intersect(userEvent._1).nonEmpty)
        .toMap
    )

  def getUserEventWithEventId(eventId: UUID): ZIO[Any, Throwable, (Set[String], UserEvent)] =
    ZIO
      .fromOption(database.find(_._2.events.contains(eventId)))
      .mapError(_ => new Exception(s"Event $eventId doesn't exist, cannot update"))

  def createUserEventFromEvent(event: Event): UIO[UserEvent] =
    ZIO.succeed(
      UserEvent(
        linkedUserIds = event.userIds,
        events = Map(event.id -> event),
        sources = Set(event.source)
      )
    )

  def removeAll(keys: Iterable[Set[String]]): ZIO[Any, Nothing, Unit] =
    ZIO.succeed(keys.foreach(database.remove))

  def insertEvent(event: Event): ZIO[Any, Nothing, Option[UserEvent]] =
    for {
      userEventFromEvent    <- createUserEventFromEvent(event)
      linkedUserEvents      <- getLinkedUserEvents(event.userIds)
      _                     <- removeAll(linkedUserEvents.keys).fork
      mergedUserEventsFiber <- UserEvent
        .mergeUserEvents(linkedUserEvents.values.toList :+ userEventFromEvent)
        .fork
      userEventsToAdd       <- mergedUserEventsFiber.join
    } yield database.put(userEventsToAdd.linkedUserIds, userEventsToAdd)

  def getUpdatedUserEvent(userEvent: UserEvent, updateEvent: UpdateEvent): Task[UserEvent] =
    ZIO.attempt {
      val eventToUpdate = userEvent.events(updateEvent.id)
      val updatedEvent  = eventToUpdate.copy(userIds = updateEvent.userIds)

      userEvent.copy(
        linkedUserIds = userEvent.linkedUserIds
          .removedAll(eventToUpdate.userIds)
          .union(updateEvent.userIds),
        events = userEvent.events.updated(eventToUpdate.id, updatedEvent)
      )
    }

  def updateEvent(updateEvent: UpdateEvent): Task[Option[UserEvent]] =
    for {
      userEvent           <- getUserEventWithEventId(updateEvent.id)
      _                   <- ZIO.succeed(database.remove(userEvent._1)).fork
      getUpdatedUserEvent <- getUpdatedUserEvent(userEvent._2, updateEvent).fork
      updatedUserEvent    <- getUpdatedUserEvent.join
    } yield database.put(updatedUserEvent.linkedUserIds, updatedUserEvent)

}

object InMemoryDatabase {

  val layer: ZLayer[Any, Nothing, InMemoryDatabase] =
    ZLayer.succeed(InMemoryDatabase(mutable.HashMap.empty[Set[String], UserEvent]))

}
