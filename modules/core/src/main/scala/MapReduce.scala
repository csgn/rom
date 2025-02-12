package rom.core

import scala.io.Source
import cats.effect._
import rom._

trait Node {
  val id: Int
  protected def run: IO[Unit]
}
case class Coordinator(val id: Int = 0) extends Node {
  def run: IO[Unit] = IO(())
}
case class Worker(val id: Int) extends Node {
  def run: IO[Unit] = IO(())
}

case class MapReduce[T, T1, T2](
  val numOfReducer: Int,
  val inputFile: String
)(val mapper: Mapper[_], val reducer: Reducer[_, _]) {
  private def readFile(filename: String, partition: Int = 3): IO[Iterator[Seq[String]]] = IO {
    Source
      .fromFile(filename)
      .getLines()
      .grouped(partition)
  }

  private def splitInput(input: Iterator[Seq[String]]): IO[(Int, Iterator[(String, Iterator[String])])] = IO {
    input.foldLeft((0, Iterator.empty[(String, Iterator[String])])) { case ((count, acc), el) =>
      (count + 1, acc ++ Iterator(((count + 1).toString, Iterator(el.mkString))))
    }
  }

  private def shuffle: IO[Unit] = ???

  def run: IO[Unit] = for {
    input <- readFile(inputFile)
    (numOfMapper: Int, splits: Iterator[(String, Iterator[String])]) <- splitInput(input)
    (key: String, values: Iterator[String]) = splits.next
    mapperResult = mapper.run(key, values)
    _ <- IO.println(mapperResult.toList)
  } yield IO(())
}
