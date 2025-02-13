package rom.core

sealed case class Reducer[_, _](
  val name: String
)(val run: (String, List[_]) => (String, _))
