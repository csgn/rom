package rom.core

import cats._
import cats.implicits._
import cats.effect._

import scala.io.{BufferedSource, Source}
import scala.concurrent.duration._

import rom._

case class MapReduce(
  val numOfReducer: Int,
  val inputFile: String
)(val mapper: Mapper[_], val reducer: Reducer[_, _]) {
  private def openFile(filename: String): Resource[IO, BufferedSource] =
    Resource.make(IO(Source.fromFile(filename)))(file => IO(file.close()))

  private def splitInput(
    input: Iterator[Seq[String]]
  ): IO[(Int, Iterator[(String, Iterator[String])])] = IO {
    input.foldLeft((0, Iterator.empty[(String, Iterator[String])])) { case ((count, acc), el) =>
      (count + 1, acc ++ Iterator(((count + 1).toString, Iterator(el.mkString))))
    }
  }

  case class MapWorker(key: String, values: Iterator[String], f: Mapper[_], d: Deferred[IO, List[(String, _)]]) {
    def run: IO[String] =
      d.complete(f.run(key, values)).as(key)
  }

  case class ReduceWorker(
    values: List[(String, List[_])],
    f: Reducer[_, _],
    d: Deferred[IO, List[(String, _)]]
  ) {
    def run: IO[Boolean] = d.complete(values.foldLeft(List.empty[(String, _)]) { case (acc, (key, value)) =>
      acc :+ f.run(key, value)
    })
  }

  case class ShuffleWorker(value: List[(String, _)], d: Deferred[IO, List[(String, List[_])]]) {
    def run: IO[Boolean] = {
      d.complete(
        value
          .foldLeft(Map.empty[String, List[_]]) { case (acc, (key, count)) =>
            acc.get(key) match {
              case None               => acc + (key -> List(count))
              case Some(keyCountList) => acc + (key -> (keyCountList :+ count))
            }
          }
          .toList
      )
    }
  }

  def run: IO[Unit] = for {
    // read and split file into chunks
    (numOfMapper: Int, splits: Iterator[(String, Iterator[String])]) <- openFile(inputFile).use { buffer =>
      splitInput(buffer.getLines().grouped(16))
    }
    // create mapper buffers
    mapperLocalBuffers <- Deferred[IO, List[(String, _)]].parReplicateA(numOfMapper)
    // create map workers
    mapWorkers = mapperLocalBuffers
      .zip(splits)
      .parTraverse { case (d, (key, values)) =>
        MapWorker(key, values, mapper, d).run
      }
    (_, intermediatePairs) <- (mapWorkers, mapperLocalBuffers.parTraverse(_.get)).parTupled

    // create shuffle buffers
    shuffleBuffers <- Deferred[IO, List[(String, List[_])]].parReplicateA(numOfMapper)
    // create shuffle workers
    shuffleWorkers = shuffleBuffers
      .zip(intermediatePairs)
      .parTraverse { case (d, value) =>
        ShuffleWorker(value, d).run
      }
    (_, shufflePairs) <- (shuffleWorkers, shuffleBuffers.parTraverse(_.get)).parTupled

    // create reducer buffers
    reducerBuffers <- Deferred[IO, List[(String, _)]].parReplicateA(numOfReducer)
    // create reducer workers
    reduceWorkers = reducerBuffers
      .zip(shufflePairs)
      .parTraverse { case (d, values) =>
        ReduceWorker(values, reducer, d).run
      }
    (_, reducerPairs) <- (reduceWorkers, reducerBuffers.parTraverse(_.get)).parTupled

    _ <- IO.println(reducerPairs)
  } yield IO(())
}
