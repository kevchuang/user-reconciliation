package com.contentsquare.model

import io.circe.{Decoder, Encoder}

object EventType extends Enumeration {
  type EventType = Value
  val display, buy = Value

  implicit val decoder: Decoder[EventType.Value] =
    Decoder.decodeEnumeration(EventType)
  implicit val encoder: Encoder[EventType.Value] =
    Encoder.encodeEnumeration(EventType)
}
