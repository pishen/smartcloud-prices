package prices

import cats.effect._
import com.comcast.ip4s._
import fs2.Stream
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger

import prices.config.Config
import prices.routes.InstanceKindRoutes
import prices.services.SmartcloudInstanceKindService
import cats.effect.std.AtomicCell
import prices.data.InstancePrice

object Server {

  def serve(config: Config): Stream[IO, ExitCode] = {

    val instanceKindService = SmartcloudInstanceKindService.make[IO](
      SmartcloudInstanceKindService.Config(
        config.smartcloud.baseUri,
        config.smartcloud.token
      )
    )

    def httpApp(prices: AtomicCell[IO, Map[String, InstancePrice]], expireInterval: Int) = (
      InstanceKindRoutes[IO](instanceKindService, prices, expireInterval).routes
    ).orNotFound

    val server = for {
      expireInterval <- instanceKindService.getExpireInterval()
      _ = println("expireInterval: " + expireInterval)
      prices <- AtomicCell[IO].of(Map.empty[String, InstancePrice])
      server <- EmberServerBuilder
        .default[IO]
        .withHost(Host.fromString(config.app.host).get)
        .withPort(Port.fromInt(config.app.port).get)
        .withHttpApp(Logger.httpApp(true, true)(httpApp(prices, expireInterval)))
        .build
        .useForever
    } yield server

    Stream.eval(server)
  }

}
