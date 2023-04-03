package com.contentsquare.service.buffer

import zio._

object RequestBuffer {
  type RequestBuffer = Service

  trait Service {
    def add: UIO[Int]
    def hasReachedLimit: UIO[Boolean]
    def remove: UIO[Int]
  }

  abstract class LiveService(buffer: Ref[Int], limit: Int) extends Service {
    override def add: UIO[Int] = buffer.updateAndGet(_ + 1)

    override def hasReachedLimit: UIO[Boolean] = buffer.get.map(_ >= limit)

    override def remove: UIO[Int] = buffer.updateAndGet(_ - 1)
  }
}
