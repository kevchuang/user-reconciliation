package com.contentsquare.utils

import com.contentsquare.database.InMemoryDatabase
import com.contentsquare.model.EventType.EventType
import com.contentsquare.model.Identifiers.{EventId, UserId, UserIdentifier}
import com.contentsquare.model.Source.Source
import com.contentsquare.model.{Event, EventType, Source, UpdateEvent, User}

import java.util.UUID
import scala.collection.mutable
import scala.util.Random

object DataGenerator {

  def generateInMemoryDatabase(
    users: List[User] = List.empty[User]
  ): InMemoryDatabase =
    InMemoryDatabase(database =
      users.foldLeft(mutable.HashMap.empty[UserIdentifier, User])((acc, user) =>
        acc.addOne(UserIdentifier(user.linkedUserIds) -> user)
      )
    )

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
    linkedUserIds: Set[UserId] = Set(UserId(UUID.randomUUID.toString)),
    events: Set[Event] = Set(generateRandomEvent()),
    sources: Set[Source] = Set(Source(Random.nextInt(Source.maxId)))
  ): User =
    User(
      linkedUserIds = linkedUserIds,
      events = events,
      sources = sources
    )

  def generateUserFromEvent(
    event: Event
  ): User =
    User(
      linkedUserIds = event.userIds,
      events = Set(event),
      sources = Set(event.source)
    )

}
