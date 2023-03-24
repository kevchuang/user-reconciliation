package com.contentsquare.database
import com.contentsquare.error.Errors.DataNotFoundException
import com.contentsquare.model.{Event, UpdateEvent, User}
import zio._

import java.util.UUID
import scala.collection.mutable

final case class InMemoryDatabase(database: mutable.HashMap[Set[String], User]) extends Database {

  private[database] def createUserWithSingleEvent(event: Event): UIO[User] =
    ZIO.succeed(
      User(
        linkedUserIds = event.userIds,
        events = Set(event),
        sources = Set(event.source)
      )
    )

  private[database] def getEventFromUser(user: User, eventId: UUID): Task[Event] =
    ZIO
      .fromOption(user.events.find(_.id == eventId))
      .mapError(_ => DataNotFoundException("Event Id not found"))

  private[database] def getLinkedUsers(userIds: Set[String]): ZIO[Any, Nothing, Map[Set[String], User]] =
    ZIO
      .succeed(
        database
          .filter(userEvent => userIds.intersect(userEvent._1).nonEmpty)
          .toMap
      )

  private[database] def getUserAssociatedToEventId(eventId: UUID): ZIO[Any, Throwable, (Set[String], User)] =
    ZIO
      .fromOption(database.find(_._2.events.exists(_.id == eventId)))
      .mapError(_ => DataNotFoundException(s"Event $eventId doesn't exist"))

  private[database] def insertMultipleEvents(events: Set[Event]): UIO[Unit] =
    ZIO
      .collectAll(events.map(insertEvent))
      .map(_ => ())

  private[database] def mergeUsers(users: List[User]): ZIO[Any, Nothing, User] =
    ZIO.succeed(
      users.foldLeft(User())((acc, user) =>
        acc.copy(
          linkedUserIds = acc.linkedUserIds.union(user.linkedUserIds),
          events = acc.events ++ user.events,
          sources = acc.sources.union(user.sources)
        )
      )
    )

  private[database] def removeUser(userId: Set[String]): ZIO[Any, Nothing, Option[User]] =
    ZIO.succeed(database.remove(userId))

  private[database] def removeUsers(usersIds: Iterable[Set[String]]): ZIO[Any, Nothing, Unit] =
    ZIO.succeed(usersIds.foreach(database.remove))

  private[database] def updateEventValues(event: Event, updatedUserIds: Set[String]): UIO[Event] =
    ZIO.succeed(event.copy(userIds = updatedUserIds))

  override def existsEvent(id: UUID): UIO[Boolean] =
    ZIO.succeed(
      database.exists(user => user._2.events.exists(_.id == id))
    )

  override def getUsers: UIO[List[User]] =
    ZIO.succeed(database.values.toList)

  override def insertEvent(event: Event): ZIO[Any, Nothing, Unit] =
    for {
      userWithSingleEvent <- createUserWithSingleEvent(event)
      linkedUsers         <- getLinkedUsers(event.userIds)
      _                   <- removeUsers(linkedUsers.keys)
      mergedUsers         <- mergeUsers(linkedUsers.values.toList :+ userWithSingleEvent)
    } yield database.put(mergedUsers.linkedUserIds, mergedUsers)

  override def updateEvent(updateEvent: UpdateEvent): Task[Unit] =
    for {
      userAssociated         <- getUserAssociatedToEventId(updateEvent.id)
      _                      <- removeUser(userAssociated._1)
      eventToUpdate          <- getEventFromUser(userAssociated._2, updateEvent.id)
      eventWithUpdatedValues <- updateEventValues(eventToUpdate, updateEvent.userIds)
      _                      <- insertMultipleEvents(
        (userAssociated._2.events - eventToUpdate) + eventWithUpdatedValues
      )
    } yield ()

}

object InMemoryDatabase {

  val layer: ZLayer[Any, Nothing, InMemoryDatabase] =
    ZLayer.succeed(InMemoryDatabase(mutable.HashMap.empty[Set[String], User]))

}
