package com.contentsquare.model

import io.circe.{Decoder, Encoder}

object Source extends Enumeration {
  type Source = Value
  val webpage, appscreen = Value

  implicit val decoder: Decoder[Source.Value] =
    Decoder.decodeEnumeration(Source)
  implicit val encoder: Encoder[Source.Value] =
    Encoder.encodeEnumeration(Source)
}
