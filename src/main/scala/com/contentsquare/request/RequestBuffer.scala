package com.contentsquare.request

import zio.{Ref, UIO, ZIO, ZLayer}

object RequestBuffer {
  type RequestBuffer = Service

  trait Service {
    def hasReachedMetricsRequestLimit: UIO[Boolean]
    def addMetricsRequestIntoBuffer(): UIO[Int]
    def removeMetricsRequestFromBuffer(): UIO[Int]
    def getMetricsRequest: UIO[Int]
    def hasReachedEventRequestLimit: UIO[Boolean]
    def addEventRequestIntoBuffer(): UIO[Int]
    def removeEventRequestFromBuffer(): UIO[Int]
  }

  final case class LiveService(eventRequestBuffer: Ref[Int], metricsRequestBuffer: Ref[Int]) extends Service {
    override def getMetricsRequest: UIO[Int] = metricsRequestBuffer.get

    override def hasReachedMetricsRequestLimit: UIO[Boolean] = metricsRequestBuffer.get.map(_ >= 1)

    override def addMetricsRequestIntoBuffer(): UIO[Int] = metricsRequestBuffer.updateAndGet(_ + 1)

    override def removeMetricsRequestFromBuffer(): UIO[Int] = metricsRequestBuffer.updateAndGet(_ - 1)

    override def hasReachedEventRequestLimit: UIO[Boolean] = eventRequestBuffer.get.map(_ >= 100)

    override def addEventRequestIntoBuffer(): UIO[Int] = eventRequestBuffer.updateAndGet(_ + 1)

    override def removeEventRequestFromBuffer(): UIO[Int] = eventRequestBuffer.updateAndGet(_ - 1)
  }

  val layer: ZLayer[Any, Nothing, RequestBuffer] = ZLayer {
    for {
      eventBuffer <- Ref.make(0)
      metricsBuffer <- Ref.make(0)
    } yield LiveService(eventBuffer, metricsBuffer)
  }

  def addMetricsRequestIntoBuffer(): ZIO[RequestBuffer, Nothing, Int] =
    ZIO.serviceWithZIO[RequestBuffer](_.addMetricsRequestIntoBuffer())

  def removeMetricsRequestFromBuffer(): ZIO[RequestBuffer, Nothing, Int] =
    ZIO.serviceWithZIO[RequestBuffer](_.removeMetricsRequestFromBuffer())

  def hasReachedMetricsRequestLimit: ZIO[RequestBuffer, Nothing, Boolean] =
    ZIO.serviceWithZIO[RequestBuffer](_.hasReachedMetricsRequestLimit)

  def hasReachedEventRequestLimit: ZIO[RequestBuffer, Nothing, Boolean] =
    ZIO.serviceWithZIO[RequestBuffer](_.hasReachedEventRequestLimit)

  def addEventRequestIntoBuffer(): ZIO[RequestBuffer, Nothing, Int] =
    ZIO.serviceWithZIO[RequestBuffer](_.addEventRequestIntoBuffer())

  def removeEventRequestFromBuffer(): ZIO[RequestBuffer, Nothing, Int] =
    ZIO.serviceWithZIO[RequestBuffer](_.removeEventRequestFromBuffer())

  def getMetricsRequest: ZIO[RequestBuffer, Nothing, Int] =
    ZIO.serviceWithZIO[RequestBuffer](_.getMetricsRequest)
}
