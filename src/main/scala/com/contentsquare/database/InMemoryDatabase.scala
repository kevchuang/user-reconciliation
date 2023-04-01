package com.contentsquare.database
import com.contentsquare.error.Errors.DataNotFoundException
import com.contentsquare.model.Identifiers.{EventId, Id, UserId, UserIdentifier}
import com.contentsquare.model.{Event, UpdateEvent, User}
import zio._

import java.util.UUID
import scala.collection.mutable

final case class InMemoryDatabase(
  links: mutable.HashMap[Id, UserIdentifier],
  users: mutable.HashMap[UserIdentifier, User]
) extends Database {

  /**
   * Returns an effect that produces an [[User]] with [[Event]] given in input
   */
  private[database] def createUserWithSingleEvent(event: Event): UIO[User] =
    ZIO.succeed(
      User(
        id = UserIdentifier(UUID.randomUUID),
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
  private[database] def getLinkedUserIdentifiers(userIds: Set[UserId]): ZIO[Any, Nothing, Set[UserIdentifier]] =
    ZIO
      .succeed(
        userIds.flatMap(userId => links.get(userId.asInstanceOf[Id]))
      )

  private[database] def getUsers(userIdentifiers: Set[UserIdentifier]): ZIO[Any, Nothing, List[User]] =
    ZIO
      .succeed(
        userIdentifiers.flatMap(users.get).toList
      )

  /**
   * Returns an effect that produces a tuple (key, [[User]]) that contains the
   * eventId given in input parameter.
   */
  private[database] def getUserAssociatedToEventId(eventId: EventId): ZIO[Any, Throwable, User] =
    ZIO
      .fromOption(
        links
          .get(eventId.asInstanceOf[Id])
          .flatMap(users.get)
      )
      .orElseFail(DataNotFoundException(s"Event $eventId doesn't exist"))

  /**
   * Returns an effect that insert multiple events by calling [[insertEvent]]
   * with the events given in input.
   */
  private[database] def insertMultipleEvents(events: Set[Event]): UIO[Unit] =
    ZIO.foreachPar(events)(insertEvent).unit

  /**
   * Returns an effect that produces an [[User]] that has merged all the users
   * given in input.
   */
  private[database] def mergeUsers(users: List[User]): ZIO[Any, Nothing, User] =
    ZIO.succeed(
      users.foldLeft(User(id = UserIdentifier(UUID.randomUUID)))((acc, user) =>
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
    ZIO.succeed(users.remove(userIdentifier))

  /**
   * Returns an effect that will remove multiple [[User]] corresponding to the
   * keys given in input by calling [[removeUser]].
   */
  private[database] def removeUsers(userIdentifiers: Iterable[UserIdentifier]): ZIO[Any, Nothing, Unit] =
    ZIO.succeed(userIdentifiers.foreach(users.remove))

  private[database] def removeIdsFromLinks(ids: Set[Id]): ZIO[Any, Nothing, Unit] =
    ZIO.succeed(ids.foreach(links.remove))

  private[database] def removeUserIdsFromLinks(userIds: Set[UserId]): ZIO[Any, Nothing, Unit] =
    ZIO.succeed(userIds.foreach(userId => links.remove(userId.asInstanceOf[Id])))

  private[database] def removeEventIdsFromLinks(events: Set[Event]): ZIO[Any, Nothing, Unit] =
    ZIO.succeed(events.foreach(event => links.remove(event.id.asInstanceOf[Id])))

  private[database] def removeUserLinks(user: User): UIO[Unit] =
    for {
      _ <- removeIdsFromLinks(user.linkedUserIds.asInstanceOf[Set[Id]])
      _ <- removeIdsFromLinks(user.events.map(_.id.asInstanceOf[Id]))
    } yield ()

  private[database] def removeUsersLinks(users: List[User]): ZIO[Any, Nothing, Unit] =
    ZIO
      .foreachPar(users)(removeUserLinks)
      .unit

  /**
   * Returns an effect that will update the [[Event]] and updatedUserIds given
   * in input and produces the updated [[Event]].
   */
  private[database] def updateEventValues(event: Event, updatedUserIds: Set[UserId]): UIO[Event] =
    ZIO.succeed(event.copy(userIds = updatedUserIds))

  private[database] def insertIdsIntoLinks(ids: Set[Id], userIdentifier: UserIdentifier): ZIO[Any, Nothing, Unit] =
    ZIO.succeed(ids.foreach(id => links.put(id, userIdentifier)))

  private[database] def insertUserLinks(user: User): UIO[Unit] = {
    for {
      _ <- insertIdsIntoLinks(user.linkedUserIds.asInstanceOf[Set[Id]], user.id)
      _ <- insertIdsIntoLinks(user.events.map(_.id.asInstanceOf[Id]), user.id)
    } yield ()
  }

//  override def existsEventWithUUID(id: UUID): UIO[Boolean] =
//    ZIO.succeed(
//      links.contains(EventId(id).asInstanceOf[Id])
//    )

  /**
   * Returns an effect that will check if the event id given in input exists in
   * the database and produces True if it exists, False if it doesn't.
   */
  override def existsEvent(id: EventId): UIO[Boolean] =
    ZIO.succeed(
      links.contains(id.asInstanceOf[Id])
    )

  override def getLinks: UIO[Map[Id, UserIdentifier]] =
    ZIO.succeed(
      links.toMap
    )

  /**
   * Returns an effect that produces a list of [[User]] present in the database.
   */
  override def getUniqueUsers: UIO[List[User]] =
    ZIO.succeed(users.values.toList)

  /**
   * Returns an effect that insert an [[Event]] given in input into database.
   * First, it creates an User with the event given. Second, it will get all the
   * users that are linked to this event. Then, it will remove them from the
   * database. Finally, it will merge them into one [[User]] and insert the
   * merged [[User]] into the database.
   */
  override def insertEvent(event: Event): ZIO[Any, Nothing, Unit] =
    for {
      userWithSingleEvent   <- createUserWithSingleEvent(event)
      linkedUserIdentifiers <- getLinkedUserIdentifiers(event.userIds)
      linkedUsers           <- getUsers(linkedUserIdentifiers)
      _                     <- removeUsers(linkedUserIdentifiers)
      _                     <- removeUsersLinks(linkedUsers)
      mergedUser            <- mergeUsers(linkedUsers :+ userWithSingleEvent)
      _                     <- insertUserLinks(mergedUser)
    } yield users.put(mergedUser.id, mergedUser)

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
      _                      <- removeUser(userAssociated.id)
      _                      <- removeUserLinks(userAssociated)
      eventToUpdate          <- getEventFromUser(userAssociated, updateEvent.id)
      eventWithUpdatedValues <- updateEventValues(eventToUpdate, updateEvent.userIds)
      _                      <- insertMultipleEvents(
        (userAssociated.events - eventToUpdate) + eventWithUpdatedValues
      )
    } yield ()

}

object InMemoryDatabase {

  /**
   * InMemoryDatabase default layer
   */
  val layer: ZLayer[Any, Nothing, InMemoryDatabase] =
    ZLayer.succeed(
      InMemoryDatabase(mutable.HashMap.empty[Id, UserIdentifier], mutable.HashMap.empty[UserIdentifier, User])
    )

}
