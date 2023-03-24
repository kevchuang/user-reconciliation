package com.contentsquare.utils

import com.contentsquare.database.InMemoryDatabase
import com.contentsquare.model.EventType.EventType
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
      users.foldLeft(mutable.HashMap.empty[Set[String], User])((acc, user) => acc.addOne(user.linkedUserIds -> user))
    )

  def generateRandomEvent(
    id: UUID = UUID.randomUUID,
    source: Source = Source(Random.nextInt(Source.maxId)),
    event: EventType = EventType(Random.nextInt(EventType.maxId)),
    userIds: Set[String] = Set(UUID.randomUUID.toString, UUID.randomUUID.toString)
  ): Event =
    Event(
      id = id,
      source = source,
      event = event,
      userIds = userIds
    )

  def generateRandomUpdateEvent(
    id: UUID = UUID.randomUUID,
    userIds: Set[String] = Set(UUID.randomUUID.toString)
  ): UpdateEvent =
    UpdateEvent(
      id = id,
      userIds = userIds
    )

  def generateRandomUser(
    linkedUserIds: Set[String] = Set(UUID.randomUUID.toString),
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
