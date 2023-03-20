package com.contentsquare.model

import com.contentsquare.model.Source.Source
import io.circe._
import io.circe.generic.semiauto._
import zio.ZIO

import java.util.UUID

final case class UserEvent(
  linkedUserIds: Set[String] = Set.empty[String],
  events: Map[UUID, Event] = Map.empty[UUID, Event],
  sources: Set[Source] = Set.empty[Source]
)

object UserEvent {
  implicit val decoder: Decoder[UserEvent] = deriveDecoder[UserEvent]
  implicit val encoder: Encoder[UserEvent] = deriveEncoder[UserEvent]

  def mergeUserEvents(userEvents: List[UserEvent]): ZIO[Any, Nothing, UserEvent] =
    ZIO.succeed(
      userEvents.foldLeft(UserEvent())((acc, userEvent) =>
        acc.copy(
          linkedUserIds = acc.linkedUserIds.union(userEvent.linkedUserIds),
          events = acc.events ++ userEvent.events,
          sources = acc.sources.union(userEvent.sources)
        )
      )
    )
}
