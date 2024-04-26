package prices.services

import cats.implicits._
import cats.effect._
import org.http4s._
import org.http4s.circe._

import prices.data._
import sttp.client3._
import sttp.client3.circe._
import sttp.capabilities.fs2.Fs2Streams

object SmartcloudInstanceKindService {

  final case class Config(
      baseUri: String,
      token: String
  )

  def make[F[_]: Concurrent](config: Config): InstanceKindService[F] = new SmartcloudInstanceKindService(config)

  private final class SmartcloudInstanceKindService[F[_]: Concurrent](
      config: Config
  ) extends InstanceKindService[F] {

    implicit val instanceKindsEntityDecoder: EntityDecoder[F, List[String]] = jsonOf[F, List[String]]

    val getAllUri = uri"${config.baseUri}/instances"

    override def getAll()(
        implicit
        backend: SttpBackend[F, Fs2Streams[F]]
    ): F[List[InstanceKind]] =
      basicRequest
        .get(getAllUri)
        .auth
        .bearer(config.token)
        .response(asJson[List[String]])
        .send(backend)
        .map { resp =>
          resp.body.toOption.get.map(InstanceKind(_))
        }

  }

}
