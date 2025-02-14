package examples

import cats._
import cats.implicits._
import cats.effect.{Deferred, ExitCode, IO}

import rom.core.{App, MapReduce, Mapper, Reducer}

object Main extends App {
  def run(args: List[String]): IO[ExitCode] = for {
    // create result ref
    result <- Deferred[IO, List[List[(String, _)]]]

    // create mapper function
    mapper = Mapper("WordCounter") { (key: String, values: List[String]) =>
      values.foldLeft(List.empty[(String, Long)]) { case (acc, value) =>
        acc ++ value.split(" ").filter(!_.isEmpty).map((_, 1L))
      }
    }

    // create reducer function
    reducer = Reducer("Adder") { (key, values) =>
      (key, values.asInstanceOf[List[Long]].sum)
    }

    // create mapreduce
    mr = MapReduce(
      numOfReducer = 2,
      inputFile = "TEST/input2.txt"
    )(mapper, reducer)

    // run mapreduce job and wait till get the result
    // (res: Boolean, output: List[List[(String, _)]]) <- (mr.run(result), result.get).parTupled
    _ <- mr.run(result)

    // _ <- IO.println((res, output.map(_.toList).toList))
  } yield ExitCode.Success
}
