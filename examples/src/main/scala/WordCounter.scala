package examples

import cats._
import cats.implicits._
import cats.effect.{ExitCode, IO, Ref}
import rom.core.{App, MapReduce, Mapper, Reducer}

object Main extends App {
  def run(args: List[String]): IO[ExitCode] = for {
    resultRef <- Ref[IO].of(Array.empty[(String, Int)])
    mapper = Mapper("WordCounter") { (key: String, value: Iterator[String]) =>
      value.next
        .split(" ")
        .filter(!_.isEmpty)
        .map((_, 1))
    }
    reducer = Reducer("Adder") { (key: String, values: Array[Int]) =>
      Array((key, values.sum))
    }
    mr = MapReduce(
      numOfReducer = 2,
      inputFile = "TEST/input2.txt"
    )(mapper, reducer)

    _ <- mr.run

    /*
     * mapperResult = Array(
     *  ("hello", 1),
     *  ("world", 1),
     *  ("how", 1),
     *  ("are", 1),
     *  ("you", 1),
     *  ("world", 1),
     *  ("hello", 1),
     * )
     */
    // mapperResult = mapper.run("doc1", "hello world how are you world hello")

    /*
     * shuffleResult = Array(
     *  ("hello", Array(1, 1)),
     *  ("world", Array(1, 1)),
     *  ("how", Array(1)),
     *  ("are", Array(1)),
     *  ("you", Array(1)),
     * )
     */
    // shuffleResult = mapperResult.foldLeft(Map.empty[String, Array[Int]]) { case (acc, (key, count)) =>
    //   acc.get(key) match {
    //     case None                => acc + (key -> Array(count))
    //     case Some(keyCountArray) => acc.updated(key, keyCountArray :+ count)
    //   }
    // }

    /*
     * reduceResult = Array(
     *  ("hello", 2),
     *  ("world", 2),
     *  ("how", 1),
     *  ("are", 1),
     *  ("you", 1),
     * )
     */
    // reduceResult = shuffleResult.map { case (key, values) =>
    //   reducer.run(key, values)
    // }
  } yield ExitCode.Success
}
