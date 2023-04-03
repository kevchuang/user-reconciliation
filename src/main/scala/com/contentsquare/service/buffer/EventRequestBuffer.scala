package com.contentsquare.service.buffer

import zio._

object EventRequestBuffer {
  type EventRequestBuffer = EventLiveService

  final case class EventLiveService(buffer: Ref[Int], limit: Int = 100) extends RequestBuffer.LiveService(buffer, limit)

  lazy val layer: ZLayer[Any, Nothing, EventRequestBuffer] = ZLayer {
    for {
      eventBuffer <- Ref.make(0)
    } yield EventLiveService(eventBuffer)
  }

  def add: ZIO[EventRequestBuffer, Nothing, Int] =
    ZIO.serviceWithZIO[EventRequestBuffer](_.add)

  def remove: ZIO[EventRequestBuffer, Nothing, Int] =
    ZIO.serviceWithZIO[EventRequestBuffer](_.remove)

  def hasReachedLimit: ZIO[EventRequestBuffer, Nothing, Boolean] =
    ZIO.serviceWithZIO[EventRequestBuffer](_.hasReachedLimit)
}
