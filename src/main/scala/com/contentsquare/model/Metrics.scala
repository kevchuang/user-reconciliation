package com.contentsquare.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class Metrics(
  uniqueUsers: Long = 0,
  bouncedUsers: Long = 0,
  crossDeviceUsers: Long = 0
)

object Metrics {
  implicit val decoder: Decoder[Metrics] = deriveDecoder[Metrics]
  implicit val encoder: Encoder[Metrics] = deriveEncoder[Metrics]
}
