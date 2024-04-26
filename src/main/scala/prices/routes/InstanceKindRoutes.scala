package prices.routes

import cats.implicits._
import cats.effect._
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

import prices.routes.protocol._
import prices.services.InstanceKindService
import sttp.client3.SttpBackend
import sttp.capabilities.fs2.Fs2Streams
import cats.effect.std.AtomicCell
import prices.data.InstancePrice

final case class InstanceKindRoutes[F[_]: Sync](
    instanceKindService: InstanceKindService[F],
    cachedPrices: AtomicCell[F, Map[String, InstancePrice]],
    expireInterval: Int
)(
    implicit
    backend: SttpBackend[F, Fs2Streams[F]]
) extends Http4sDsl[F] {

  val prefix = "/instance-kinds"

  implicit val instanceKindResponseEncoder = jsonEncoderOf[F, List[InstanceKindResponse]]

  private val get: HttpRoutes[F] = HttpRoutes.of {
    case GET -> Root =>
      instanceKindService.getAll().flatMap(kinds => Ok(kinds.map(k => InstanceKindResponse(k))))
  }

  def routes: HttpRoutes[F] =
    Router(
      prefix -> get
    )

}
