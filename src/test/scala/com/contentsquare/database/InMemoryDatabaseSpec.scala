package com.contentsquare.database

import com.contentsquare.error.Errors.DataNotFoundException
import com.contentsquare.model.Identifiers._
import com.contentsquare.model.User
import com.contentsquare.utils.DataGenerator
import zio.test.Assertion._
import zio.test._
import zio.{Scope, ZIO}

import java.util.UUID
import scala.collection.mutable

object InMemoryDatabaseSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    createUserWithSingleEventSuite +
      getEventFromUserSuite +
      getLinkedUsersSuite +
      getUserAssociatedToEventIdSuite +
      mergeUsersSuite +
      removeUserSuite +
      removeUsersSuite +
      updateEventValuesSuite +
      existsEventSuite +
      getUsersSuite +
      insertEventSuite +
      updateEventSuite

  private lazy val createUserWithSingleEventSuite =
    suite("createUserWithSingleEvent")(
      test("should return an User with event gave as input") {
        val database = InMemoryDatabase(mutable.HashMap.empty[UserIdentifier, User])
        val event    = DataGenerator.generateRandomEvent()
        val expected = DataGenerator.generateRandomUser(event.userIds, Set(event), Set(event.source))

        assertZIO(database.createUserWithSingleEvent(event))(equalTo(expected))
      }
    )

  private lazy val getEventFromUserSuite =
    suite("getEventFromUser")(
      test("should return an Event in User") {
        val database = InMemoryDatabase(mutable.HashMap.empty[UserIdentifier, User])
        val event    = DataGenerator.generateRandomEvent()
        val user     = DataGenerator.generateRandomUser(event.userIds, Set(event), Set(event.source))

        assertZIO(database.getEventFromUser(user, event.id))(equalTo(event))
      },
      test("should fail with DataNotFoundException if User doesn't contain the event") {
        val database = InMemoryDatabase(mutable.HashMap.empty[UserIdentifier, User])
        val user     = DataGenerator.generateRandomUser()

        assertZIO(database.getEventFromUser(user, EventId(UUID.randomUUID)).exit)(
          fails(isSubtype[DataNotFoundException](anything))
        )
      }
    )

  private lazy val getLinkedUsersSuite =
    suite("getLinkedUsers")(
      test("should return a Map containing Users that share the same userIds") {
        val sharedId   = UserId(UUID.randomUUID.toString)
        val firstUser  =
          DataGenerator.generateRandomUser(linkedUserIds = Set(UserId(UUID.randomUUID.toString), sharedId))
        val secondUser = DataGenerator.generateRandomUser()
        val thirdUser  =
          DataGenerator.generateRandomUser(linkedUserIds = Set(UserId(UUID.randomUUID.toString), sharedId))
        val database   = DataGenerator.generateInMemoryDatabase(List(firstUser, secondUser, thirdUser))
        val expected   = Map(
          UserIdentifier(firstUser.linkedUserIds) -> firstUser,
          UserIdentifier(thirdUser.linkedUserIds) -> thirdUser
        )

        assertZIO(database.getLinkedUsers(Set(sharedId)))(equalTo(expected))
      },
      test("should return an empty Map if there is no linked users") {
        val firstUser  = DataGenerator.generateRandomUser()
        val secondUser = DataGenerator.generateRandomUser()
        val thirdUser  = DataGenerator.generateRandomUser()
        val database   = DataGenerator.generateInMemoryDatabase(List(firstUser, secondUser, thirdUser))
        val expected   = Map.empty[UserIdentifier, User]

        assertZIO(database.getLinkedUsers(Set(UserId(UUID.randomUUID.toString))))(equalTo(expected))
      }
    )

  private lazy val getUserAssociatedToEventIdSuite =
    suite("getUserAssociatedToEventId")(
      test("should return a User containing the eventId") {
        val event      = DataGenerator.generateRandomEvent()
        val firstUser  = DataGenerator.generateRandomUser(
          linkedUserIds = event.userIds,
          events = Set(event, DataGenerator.generateRandomEvent())
        )
        val secondUser = DataGenerator.generateRandomUser()
        val thirdUser  = DataGenerator.generateRandomUser()
        val database   = DataGenerator.generateInMemoryDatabase(List(firstUser, secondUser, thirdUser))
        val expected   = (UserIdentifier(event.userIds), firstUser)

        assertZIO(database.getUserAssociatedToEventId(event.id))(equalTo(expected))
      },
      test("should fail with DataNotFoundException if there is no user containing the event") {
        val firstUser  = DataGenerator.generateRandomUser()
        val secondUser = DataGenerator.generateRandomUser()
        val database   = DataGenerator.generateInMemoryDatabase(List(firstUser, secondUser))

        assertZIO(database.getUserAssociatedToEventId(EventId(UUID.randomUUID)).exit)(
          fails(isSubtype[DataNotFoundException](anything))
        )
      }
    )

  private lazy val mergeUsersSuite =
    suite("mergeUsers")(
      test("should merge users into one User") {
        val firstUser  = DataGenerator.generateRandomUser()
        val secondUser = DataGenerator.generateRandomUser()
        val thirdUser  = DataGenerator.generateRandomUser()
        val database   = DataGenerator.generateInMemoryDatabase(List(firstUser, secondUser, thirdUser))
        val expected   = User(
          linkedUserIds = firstUser.linkedUserIds.union(secondUser.linkedUserIds).union(thirdUser.linkedUserIds),
          events = firstUser.events.union(secondUser.events).union(thirdUser.events),
          sources = firstUser.sources.union(secondUser.sources).union(thirdUser.sources)
        )

        assertZIO(database.mergeUsers(List(firstUser, secondUser, thirdUser)))(equalTo(expected))
      },
      test("should return an empty User if an empty is given as input") {
        val database = DataGenerator.generateInMemoryDatabase()
        val expected = User()

        assertZIO(database.mergeUsers(List.empty[User]))(equalTo(expected))
      }
    )

  private lazy val removeUserSuite =
    suite("removeUser")(
      test("should remove user from database and return the removed user") {
        val firstUser  = DataGenerator.generateRandomUser()
        val secondUser = DataGenerator.generateRandomUser()
        val database   = DataGenerator.generateInMemoryDatabase(List(firstUser, secondUser))

        for {
          existsInDatabase <- ZIO.succeed(database.database.exists(_._1 == firstUser.linkedUserIds))
          removedUser      <- database.removeUser(UserIdentifier(firstUser.linkedUserIds))
          isRemoved        <- ZIO.succeed(!database.database.exists(_._1 == firstUser.linkedUserIds))
        } yield assert(removedUser)(equalTo(Option(firstUser))) &&
          assertTrue(existsInDatabase, isRemoved)
      },
      test("should return none if user isn't in database") {
        val firstUser  = DataGenerator.generateRandomUser()
        val secondUser = DataGenerator.generateRandomUser()
        val database   = DataGenerator.generateInMemoryDatabase(List(firstUser))

        assertZIO(database.removeUser(UserIdentifier(secondUser.linkedUserIds)))(equalTo(None))
      }
    )

  private lazy val removeUsersSuite =
    suite("removeUsers")(
      test("should remove multiple users from database") {
        val firstUser  = DataGenerator.generateRandomUser()
        val secondUser = DataGenerator.generateRandomUser()
        val database   = DataGenerator.generateInMemoryDatabase(List(firstUser, secondUser))

        for {
          firstUserExists     <- ZIO.succeed(database.database.exists(_._1 == firstUser.linkedUserIds))
          secondUserExists    <- ZIO.succeed(database.database.exists(_._1 == secondUser.linkedUserIds))
          _                   <- database.removeUsers(
            List(UserIdentifier(firstUser.linkedUserIds), UserIdentifier(secondUser.linkedUserIds))
          )
          firstUserIsRemoved  <- ZIO.succeed(!database.database.exists(_._1 == firstUser.linkedUserIds))
          secondUserIsRemoved <- ZIO.succeed(!database.database.exists(_._1 == secondUser.linkedUserIds))
        } yield assertTrue(firstUserExists, secondUserExists, firstUserIsRemoved, secondUserIsRemoved)
      }
    )

  private lazy val updateEventValuesSuite =
    suite("updateEventValues")(
      test("should update userIds field in Event") {
        val event    = DataGenerator.generateRandomEvent()
        val userIds  = Set(UserId(UUID.randomUUID.toString))
        val expected = event.copy(userIds = userIds)
        val database = DataGenerator.generateInMemoryDatabase()

        assertZIO(database.updateEventValues(event, userIds))(equalTo(expected))
      }
    )

  private lazy val existsEventSuite =
    suite("existsEvent")(
      test("should return true if event exists in database") {
        val event    = DataGenerator.generateRandomEvent()
        val user     = DataGenerator.generateRandomUser(linkedUserIds = event.userIds, events = Set(event))
        val database = DataGenerator.generateInMemoryDatabase(List(user))

        assertZIO(database.existsEvent(event.id))(equalTo(true))
      },
      test("should return false if event doesn't exist in database") {
        val event    = DataGenerator.generateRandomEvent()
        val user     = DataGenerator.generateRandomUser(linkedUserIds = event.userIds, events = Set(event))
        val database = DataGenerator.generateInMemoryDatabase(List(user))

        assertZIO(database.existsEvent(DataGenerator.generateRandomEvent().id))(equalTo(false))
      }
    )

  private lazy val getUsersSuite =
    suite("getUsers")(
      test("should return a list of Users from database") {
        val firstUser  = DataGenerator.generateRandomUser()
        val secondUser = DataGenerator.generateRandomUser()
        val thirdUser  = DataGenerator.generateRandomUser()
        val expected   = List(firstUser, secondUser, thirdUser)
        val database   = DataGenerator.generateInMemoryDatabase(expected)

        assertZIO(database.getUsers)(hasSameElements(expected))
      }
    )

  private lazy val insertEventSuite =
    suite("insertEvent")(
      test("should insert an event into database") {
        val event    = DataGenerator.generateRandomEvent()
        val database = DataGenerator.generateInMemoryDatabase()

        for {
          _             <- database.insertEvent(event)
          eventInserted <- database.existsEvent(event.id)
        } yield assertTrue(eventInserted)
      },
      test("should insert an event by merging linked users") {
        val firstLink  = UserId(UUID.randomUUID.toString)
        val secondLink = UserId(UUID.randomUUID.toString)
        val event      = DataGenerator.generateRandomEvent(userIds = Set(firstLink, secondLink))
        val firstUser  =
          DataGenerator.generateRandomUser(linkedUserIds = Set(firstLink, UserId(UUID.randomUUID.toString)))
        val secondUser =
          DataGenerator.generateRandomUser(linkedUserIds = Set(secondLink, UserId(UUID.randomUUID.toString)))
        val database   = DataGenerator.generateInMemoryDatabase(List(firstUser, secondUser))
        val expected   = User(
          linkedUserIds = event.userIds.union(firstUser.linkedUserIds).union(secondUser.linkedUserIds),
          events = firstUser.events.union(secondUser.events) + event,
          sources = firstUser.sources.union(secondUser.sources) + event.source
        )

        for {
          _             <- database.insertEvent(event)
          eventInserted <- database.existsEvent(event.id)
          users         <- database.getUsers
        } yield assertTrue(eventInserted) &&
          assert(users)(hasSameElements(List(expected)))
      }
    )

  private lazy val updateEventSuite =
    suite("updateEvent")(
      test("should update event in database") {
        val event        = DataGenerator.generateRandomEvent()
        val updateEvent  = DataGenerator.generateRandomUpdateEvent(id = event.id)
        val updatedEvent = event.copy(userIds = updateEvent.userIds)
        val database     = DataGenerator.generateInMemoryDatabase()
        val initialUser  = DataGenerator.generateUserFromEvent(event)
        val updatedUser  = DataGenerator.generateUserFromEvent(updatedEvent)

        for {
          _            <- database.insertEvent(event)
          initialUsers <- database.getUsers
          _            <- database.updateEvent(updateEvent)
          updatedUsers <- database.getUsers
        } yield assert(initialUsers)(hasSameElements(List(initialUser))) &&
          assert(updatedUsers)(hasSameElements(List(updatedUser)))
      },
      test("should update event and remake event linking") {
        val firstLink  = UserId(UUID.randomUUID.toString)
        val secondLink = UserId(UUID.randomUUID.toString)

        val event       = DataGenerator.generateRandomEvent(userIds = Set(firstLink, secondLink))
        val firstEvent  = DataGenerator.generateRandomEvent(userIds = Set(firstLink, UserId(UUID.randomUUID.toString)))
        val secondEvent = DataGenerator.generateRandomEvent(userIds = Set(secondLink, UserId(UUID.randomUUID.toString)))

        val firstUser   = DataGenerator.generateUserFromEvent(firstEvent)
        val secondUser  = DataGenerator.generateUserFromEvent(secondEvent)
        val database    = DataGenerator.generateInMemoryDatabase(List(firstUser, secondUser))
        val updateEvent = DataGenerator.generateRandomUpdateEvent(id = event.id)

        val expectedUsers = List(
          firstUser,
          secondUser,
          DataGenerator.generateUserFromEvent(event.copy(userIds = updateEvent.userIds))
        )

        for {
          _            <- database.insertEvent(event)
          hasOneUser   <- database.getUsers.map(_.length == 1)
          _            <- database.updateEvent(updateEvent)
          updatedUsers <- database.getUsers
        } yield assertTrue(hasOneUser) &&
          assert(updatedUsers)(hasSameElements(expectedUsers))
      },
      test("should fail if update event id is not in database") {
        val updateEvent = DataGenerator.generateRandomUpdateEvent()
        val database    = DataGenerator.generateInMemoryDatabase()

        assertZIO(database.updateEvent(updateEvent).exit)(fails(isSubtype[DataNotFoundException](anything)))
      }
    )
}
