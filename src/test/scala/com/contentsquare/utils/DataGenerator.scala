package com.contentsquare.utils

import com.contentsquare.database.InMemoryDatabase
import com.contentsquare.model.EventType.EventType
import com.contentsquare.model.Identifiers._
import com.contentsquare.model.Source.Source
import com.contentsquare.model._

import java.util.UUID
import scala.collection.mutable
import scala.util.Random

object DataGenerator {

  def generateInMemoryDatabase(
    users: List[User] = List.empty[User]
  ): InMemoryDatabase = {
    val links = mutable.HashMap.empty[Id, UserIdentifier]
    val usersMap = users.foldLeft(mutable.HashMap.empty[UserIdentifier, User])((acc, user) => acc.addOne(user.id, user))
    usersMap.foreach { case (userIdentifier, user) =>
      user.linkedUserIds.foreach(userId => links.put(userId.asInstanceOf[Id], userIdentifier))
      user.events.foreach(event => links.put(event.id.asInstanceOf[Id], userIdentifier))
    }

    InMemoryDatabase(
      links = links,
      users = usersMap
    )
  }

  def generateRandomEvent(
    id: EventId = EventId(UUID.randomUUID),
    source: Source = Source(Random.nextInt(Source.maxId)),
    event: EventType = EventType(Random.nextInt(EventType.maxId)),
    userIds: Set[UserId] = Set(UserId(UUID.randomUUID.toString), UserId(UUID.randomUUID.toString))
  ): Event =
    Event(
      id = id,
      source = source,
      event = event,
      userIds = userIds
    )

  def generateRandomUpdateEvent(
    id: EventId = EventId(UUID.randomUUID),
    userIds: Set[UserId] = Set(UserId(UUID.randomUUID.toString))
  ): UpdateEvent =
    UpdateEvent(
      id = id,
      userIds = userIds
    )

  def generateRandomUser(
    id: UserIdentifier = UserIdentifier(UUID.randomUUID),
    linkedUserIds: Set[UserId] = Set(UserId(UUID.randomUUID.toString)),
    events: Set[Event] = Set(generateRandomEvent()),
    sources: Set[Source] = Set(Source(Random.nextInt(Source.maxId)))
  ): User =
    User(
      id = id,
      linkedUserIds = linkedUserIds,
      events = events,
      sources = sources
    )

  def generateUserFromEvent(
    event: Event
  ): User =
    User(
      id = UserIdentifier(UUID.randomUUID),
      linkedUserIds = event.userIds,
      events = Set(event),
      sources = Set(event.source)
    )

}
