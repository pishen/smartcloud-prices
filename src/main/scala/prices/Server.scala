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

object Server {

  def serve(config: Config): Stream[IO, ExitCode] = {

    val instanceKindService = SmartcloudInstanceKindService.make[IO](
      SmartcloudInstanceKindService.Config(
        config.smartcloud.baseUri,
        config.smartcloud.token
      )
    )

    def httpApp(
        implicit
        backend: SttpBackend[IO, Fs2Streams[IO]]
    ) = (
      InstanceKindRoutes[IO](instanceKindService).routes
    ).orNotFound

    val server = Http4sBackend.usingDefaultEmberClientBuilder[IO]().use { backend =>
      EmberServerBuilder
        .default[IO]
        .withHost(Host.fromString(config.app.host).get)
        .withPort(Port.fromInt(config.app.port).get)
        .withHttpApp(Logger.httpApp(true, true)(httpApp(backend)))
        .build
        .useForever
    }

    Stream.eval(server)
  }

}
