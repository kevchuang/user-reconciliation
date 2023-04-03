package com.contentsquare.service.buffer

import zio._

object MetricsRequestBuffer {
  type MetricsRequestBuffer = MetricsLiveService

  final case class MetricsLiveService(buffer: Ref[Int], limit: Int = 1) extends RequestBuffer.LiveService(buffer, limit)

  lazy val layer: ZLayer[Any, Nothing, MetricsRequestBuffer] = ZLayer {
    for {
      metricsBuffer <- Ref.make(0)
    } yield MetricsLiveService(metricsBuffer)
  }

  def add: ZIO[MetricsRequestBuffer, Nothing, Int] =
    ZIO.serviceWithZIO[MetricsRequestBuffer](_.add)

  def remove: ZIO[MetricsRequestBuffer, Nothing, Int] =
    ZIO.serviceWithZIO[MetricsRequestBuffer](_.remove)

  def hasReachedLimit: ZIO[MetricsRequestBuffer, Nothing, Boolean] =
    ZIO.serviceWithZIO[MetricsRequestBuffer](_.hasReachedLimit)
}
