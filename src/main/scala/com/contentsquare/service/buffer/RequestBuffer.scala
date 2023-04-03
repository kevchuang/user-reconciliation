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

    /**
     * Returns an effect that will add one into the buffer and get the count.
     */
    override def add: UIO[Int] = buffer.updateAndGet(_ + 1)

    /**
     * Returns an effect that will produce a boolean. If the buffer has reached
     * its limit, it produces true and false if it hasn't.
     */
    override def hasReachedLimit: UIO[Boolean] = buffer.get.map(_ >= limit)

    /**
     * Returns an effect that will remove one from the buffer and get the count.
     */
    override def remove: UIO[Int] = buffer.updateAndGet(_ - 1)
  }
}
