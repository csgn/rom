package object rom {
  implicit class DebugHelper[A](ioa: cats.effect.IO[A]) {
    def dbg: cats.effect.IO[A] =
      for {
        a <- ioa
        tn = Thread.currentThread.getName
        _ = println(s"[${tn}] $a")
      } yield a
  }
}
