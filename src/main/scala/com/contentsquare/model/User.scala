package com.contentsquare.model

import com.contentsquare.model.Source.Source
import io.circe._
import io.circe.generic.semiauto._

final case class User(
  linkedUserIds: Set[String] = Set.empty[String],
  events: Set[Event] = Set.empty[Event],
  sources: Set[Source] = Set.empty[Source]
)

object User {
  implicit val decoder: Decoder[User] = deriveDecoder[User]
  implicit val encoder: Encoder[User] = deriveEncoder[User]
}
