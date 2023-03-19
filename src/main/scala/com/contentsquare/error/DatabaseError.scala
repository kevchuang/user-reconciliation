package com.contentsquare.error

sealed trait DatabaseError

object DatabaseError {
  case class InvalidInput(message: String) extends DatabaseError
}