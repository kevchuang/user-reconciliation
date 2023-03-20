package com.contentsquare.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class Metrics(
  uniqueUsers: Long = 0,
  bouncedUsers: Long = 0,
  crossDeviceUsers: Long = 0
)

object Metrics {
  implicit val decoder: Decoder[Metrics] = deriveDecoder[Metrics]
  implicit val encoder: Encoder[Metrics] = deriveEncoder[Metrics]
}
