package com.contentsquare.error

object Errors {
  final case class DataNotFoundException(message: String) extends Exception
  final case class EmptyValueException(message: String)   extends Exception
  final case class InvalidInputDataException(message: String)      extends Exception
}
