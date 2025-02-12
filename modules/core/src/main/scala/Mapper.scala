package rom.core

import cats.Id

sealed case class Mapper[R](
  val name: String
)(val run: (String, Iterator[String]) => Array[(String, R)])
