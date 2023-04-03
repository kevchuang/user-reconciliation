package com.contentsquare.database
import com.contentsquare.error.Errors.DataNotFoundException
import com.contentsquare.model.Identifiers.{EventId, UserId, UserIdentifier}
import com.contentsquare.model.{Event, UpdateEvent, User}
import zio._

import scala.collection.mutable

final case class InMemoryDatabase(database: mutable.HashMap[UserIdentifier, User]) extends Database {

  /**
   * Returns an effect that produces an [[User]] with [[Event]] given in input
   */
  private[database] def createUserWithSingleEvent(event: Event): UIO[User] =
    ZIO.succeed(
      User(
        linkedUserIds = event.userIds,
        events = Set(event),
        sources = Set(event.source)
      )
    )

  /**
   * Returns an effect that produces [[Event]] found in [[User]] by searching
   * eventId given in input. It may fail with [[DataNotFoundException]] if
   * eventId is not in [[User]].
   */
  private[database] def getEventFromUser(user: User, eventId: EventId): Task[Event] =
    ZIO
      .fromOption(user.events.find(_.id == eventId))
      .orElseFail(DataNotFoundException(s"Event $eventId not found"))

  /**
   * Returns an effect that produces a map of users that have at one userId in
   * common with the input parameter.
   */
  private[database] def getLinkedUsers(userIds: Set[UserId]): ZIO[Any, Nothing, Map[UserIdentifier, User]] =
    ZIO
      .succeed(
        database.filter { case (userIdentifier, _) => userIds.intersect(userIdentifier).nonEmpty }.toMap
      )

  /**
   * Returns an effect that produces a tuple (key, [[User]]) that contains the
   * eventId given in input parameter.
   */
  private[database] def getUserAssociatedToEventId(eventId: EventId): ZIO[Any, Throwable, (UserIdentifier, User)] =
    ZIO
      .fromOption(database.find { case (_, user) =>
        user.events.exists(_.id == eventId)
      })
      .orElseFail(DataNotFoundException(s"Event $eventId doesn't exist"))

  /**
   * Returns an effect that insert multiple events by calling [[insertEvent]]
   * with the events given in input.
   */
  private[database] def insertMultipleEvents(events: Set[Event]): UIO[Unit] =
    ZIO.foreach(events)(insertEvent).unit

  /**
   * Returns an effect that produces an [[User]] that has merged all the users
   * given in input.
   */
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

  /**
   * Returns an effect that will remove the [[User]] corresponding to the key
   * given in input.
   */
  private[database] def removeUser(userIdentifier: UserIdentifier): ZIO[Any, Nothing, Option[User]] =
    ZIO.succeed(database.remove(userIdentifier))

  /**
   * Returns an effect that will remove multiple [[User]] corresponding to the
   * keys given in input by calling [[removeUser]].
   */
  private[database] def removeUsers(usersIdentifiers: Iterable[UserIdentifier]): ZIO[Any, Nothing, Unit] =
    ZIO.succeed(usersIdentifiers.foreach(database.remove))

  /**
   * Returns an effect that will update the [[Event]] and updatedUserIds given
   * in input and produces the updated [[Event]].
   */
  private[database] def updateEventValues(event: Event, updatedUserIds: Set[UserId]): UIO[Event] =
    ZIO.succeed(event.copy(userIds = updatedUserIds))

  /**
   * Returns an effect that will check if the event id given in input exists in
   * the database and produces True if it exists, False if it doesn't.
   */
  override def existsEvent(id: EventId): UIO[Boolean] =
    ZIO.succeed(
      database.exists { case (_, user) =>
        user.events.exists(_.id == id)
      }
    )

  /**
   * Returns an effect that produces a list of [[User]] present in the database.
   */
  override def getUsers: UIO[List[User]] =
    ZIO.succeed(database.values.toList)

  /**
   * Returns an effect that insert an [[Event]] given in input into database.
   * First, it creates an User with the event given. Second, it will get all the
   * users that are linked to this event. Then, it will remove them from the
   * database. Finally, it will merge them into one [[User]] and insert the
   * merged [[User]] into the database.
   */
  override def insertEvent(event: Event): ZIO[Any, Nothing, Unit] =
    for {
      userWithSingleEvent <- createUserWithSingleEvent(event)
      linkedUsers         <- getLinkedUsers(event.userIds)
      _                   <- removeUsers(linkedUsers.keys)
      mergedUsers         <- mergeUsers(linkedUsers.values.toList :+ userWithSingleEvent)
    } yield database.put(UserIdentifier(mergedUsers.linkedUserIds), mergedUsers)

  /**
   * Returns an effect that update an [[Event]] with an [[UpdateEvent]] given in
   * input into database. First, it gets the User that contains the [[Event]]
   * that need to be update. Second, it will remove the user that contains the
   * [[Event]] from the database. Then, it will update the concerning [[Event]]
   * with the updated values in [[UpdateEvent]] Finally, it will reinsert all
   * the events contained in the user associated so it makes sure that the
   * linking with the updated [[Event]] is correct.
   */
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

  /**
   * InMemoryDatabase default layer
   */
  val layer: ZLayer[Any, Nothing, InMemoryDatabase] =
    ZLayer.succeed(InMemoryDatabase(mutable.HashMap.empty[UserIdentifier, User]))

}
