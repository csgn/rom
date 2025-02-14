package rom.core

sealed case class Mapper[_](
  val name: String
)(val run: (String, List[String]) => List[(String, _)])
