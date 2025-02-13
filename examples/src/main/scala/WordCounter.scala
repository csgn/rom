package examples

import cats._
import cats.implicits._
import cats.effect.{ExitCode, IO, Ref}
import rom.core.{App, MapReduce, Mapper, Reducer}

object Main extends App {
  def run(args: List[String]): IO[ExitCode] = for {
    resultRef <- Ref[IO].of(List.empty[(String, Int)])
    mapper = Mapper("WordCounter") { (key: String, value: Iterator[String]) =>
      value.next
        .split(" ")
        .filter(!_.isEmpty)
        .map((_, 1))
        .toList
    }
    reducer = Reducer("Adder") { (key, values) =>
      (key, values.asInstanceOf[List[Int]].sum)
    }
    mr = MapReduce(
      numOfReducer = 2,
      inputFile = "TEST/input3.txt"
    )(mapper, reducer)

    _ <- mr.run
  } yield ExitCode.Success
}
