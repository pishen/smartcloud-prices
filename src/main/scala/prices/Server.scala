package prices

import cats.effect._
import com.comcast.ip4s._
import fs2.Stream
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger

import prices.config.Config
import prices.routes.InstanceKindRoutes
import prices.services.SmartcloudInstanceKindService
import sttp.client3.SttpBackend
import sttp.client3.http4s.Http4sBackend
import sttp.capabilities.fs2.Fs2Streams
import cats.effect.std.AtomicCell
import prices.data.InstancePriceWithTime

object Server {

  def serve(config: Config): Stream[IO, ExitCode] = {

    val instanceKindService = SmartcloudInstanceKindService.make[IO](
      SmartcloudInstanceKindService.Config(
        config.smartcloud.baseUri,
        config.smartcloud.token
      )
    )

    def httpApp(cachedPrices: AtomicCell[IO, Map[String, InstancePriceWithTime]], expireInterval: Int)(
        implicit
        backend: SttpBackend[IO, Fs2Streams[IO]]
    ) = (
      InstanceKindRoutes[IO](instanceKindService, cachedPrices, expireInterval).routes
    ).orNotFound

    val server = Http4sBackend.usingDefaultEmberClientBuilder[IO]().use { backend =>
      for {
        expireInterval <- config.app.expireInterval.map(IO.pure).getOrElse(instanceKindService.getExpireInterval()(backend))
        _ = println("expireInterval: " + expireInterval)
        cachedPrices <- AtomicCell[IO].of(Map.empty[String, InstancePriceWithTime])
        server <- EmberServerBuilder
                    .default[IO]
                    .withHost(Host.fromString(config.app.host).get)
                    .withPort(Port.fromInt(config.app.port).get)
                    .withHttpApp(Logger.httpApp(true, true)(httpApp(cachedPrices, expireInterval)(backend)))
                    .build
                    .useForever
      } yield server
    }

    Stream.eval(server)
  }

}
