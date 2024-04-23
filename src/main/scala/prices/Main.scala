package prices

import cats.effect.{ IO, IOApp }

import prices.config.Config

object Main extends IOApp.Simple {
  def run: IO[Unit] = Config.load[IO].flatMap(Server.serve(_).compile.drain)

}
