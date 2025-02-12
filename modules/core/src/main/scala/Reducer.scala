package rom.core

import cats.Id

sealed case class Reducer[T, R](
  val name: String
)(val run: (String, Array[T]) => Array[R])
