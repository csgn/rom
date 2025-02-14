package rom.core

import cats._
import cats.implicits._
import cats.effect._

import scala.io.{BufferedSource, Source}

import rom._

case class MapWorker(
  key: String,
  values: List[String],
  f: Mapper[_],
  buffer: Deferred[IO, List[(String, _)]]
) {
  def run: IO[String] = buffer.complete(f.run(key, values)).as(key)
}

case class ShuffleWorker(
  intermediateValues: List[(String, _)],
  buffer: Deferred[IO, Map[String, List[_]]]
) {
  def run: IO[Boolean] = buffer.complete {
    intermediateValues
      .foldLeft(Map.empty[String, List[_]]) { case (acc, (key, count)) =>
        acc.get(key) match {
          case None               => acc + (key -> List(count))
          case Some(keyCountList) => acc + (key -> (keyCountList.prepended(count)))
        }
      }
  }
}

case class MergeWorker(
  shuffledValue: Map[String, List[_]],
  ref: Ref[IO, Map[String, List[_]]]
) {
  def run: IO[List[Unit]] =
    shuffledValue.toList.parTraverse { case (key, value) =>
      for {
        hmap <- ref.get
        _ <- hmap.get(key) match {
          case None             => ref.update(_ + (key -> value))
          case Some(existValue) => ref.update(_ + (key -> (existValue ::: value)))
        }
      } yield ()
    }
}

case class MapReduce(
  val numOfReducer: Long,
  val inputFile: String
)(val mapper: Mapper[_], val reducer: Reducer[_, _]) {
  private def openFile(filename: String): Resource[IO, BufferedSource] =
    Resource.make(IO(Source.fromFile(filename)))(file => IO(file.close()))

  private def splitInput(
    input: Iterator[List[String]]
  ): IO[(Long, List[(String, List[String])])] = IO {
    input.foldLeft((0L, List.empty[(String, List[String])])) { case ((count, acc), el) =>
      (count + 1, acc ++ List(((count + 1).toString, List(el.mkString))))
    }
  }

  def run(result: Deferred[IO, List[List[(String, _)]]]): IO[Unit] = for {
    // read and split file into group
    (numOfMapper: Long, splits: List[(String, List[String])]) <- openFile(inputFile).use { buffer =>
      val lines = buffer.getLines().toList
      splitInput(lines.grouped(1024))
    }

    // Map Phase
    // create map workers
    mapWorkers <- splits.traverse { case (key: String, values: List[String]) =>
      Deferred[IO, List[(String, _)]].map(buffer => MapWorker(key, values, mapper, buffer))
    }

    (_, intermediatePairs) <- (
      // Run map workers on parallel
      mapWorkers.parTraverse(_.run),
      // It will wait till result is available (The `get` function doesn't block its own thread on OS level,
      // but semantically blocked.)
      mapWorkers.parTraverse(
        _.buffer.get
      )
    ).parTupled

    // Shuffle Phase
    // create shuffle workers
    shuffleWorkers <- intermediatePairs.traverse { case (values: List[(String, _)]) =>
      Deferred[IO, Map[String, List[_]]].map(buffer => ShuffleWorker(values, buffer))
    }

    (_, shuffledPairs) <- (
      shuffleWorkers.parTraverse(_.run),
      shuffleWorkers.parTraverse(_.buffer.get)
    ).parTupled

    // Merge Phase
    ref <- Ref[IO].of(Map.empty[String, List[_]])
    mergeWorkers = shuffledPairs.map { case (value: Map[String, List[_]]) =>
      MergeWorker(value, ref)
    }

    _ <- mergeWorkers.parTraverse_(_.run)
    res <- ref.get
    _ <- IO(res).dbg

    // Reduce Phase
    // r <- result.complete(IO(()))
  } yield ()
}
