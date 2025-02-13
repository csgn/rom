package rom.core

sealed case class Mapper[_](
  val name: String
)(val run: (String, Iterator[String]) => List[(String, _)])
