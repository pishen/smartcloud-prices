package prices.services

import cats.implicits._
import cats.effect._
import org.http4s._
import org.http4s.circe._

import prices.data._
import sttp.client3._
import sttp.client3.circe._
import sttp.capabilities.fs2.Fs2Streams
import retry._
import retry.RetryDetails._
import scala.concurrent.duration._
import io.circe.generic.auto._

object SmartcloudInstanceKindService {

  final case class Config(
      baseUri: String,
      token: String
  )

  def make[F[_]: Async](config: Config): InstanceKindService[F] = new SmartcloudInstanceKindService(config)

  private final class SmartcloudInstanceKindService[F[_]: Async](
      config: Config
  ) extends InstanceKindService[F] {

    implicit val instanceKindsEntityDecoder: EntityDecoder[F, List[String]] = jsonOf[F, List[String]]

    val retryPolicy = RetryPolicies.limitRetries[F](10) join RetryPolicies.exponentialBackoff[F](100.milliseconds)

    def logError(err: Throwable, details: RetryDetails): F[Unit] = details match {
      case WillDelayAndRetry(_, retriesSoFar: Int, _) =>
        s"Failed with $err. Retried $retriesSoFar times.".pure[F].map(println)
      case GivingUp(totalRetries: Int, _) =>
        s"Failed with $err. Giving up after retrying $totalRetries times.".pure[F].map(println)
    }

    val getAllUri = uri"${config.baseUri}/instances"

    override def getAll()(
        implicit
        backend: SttpBackend[F, Fs2Streams[F]]
    ): F[List[InstanceKind]] = retryingOnAllErrors(retryPolicy, logError) {
      basicRequest
        .get(getAllUri)
        .auth
        .bearer(config.token)
        .response(asJson[List[String]])
        .send(backend)
        .map { resp =>
          resp.body.toTry.get.map(InstanceKind(_))
        }
    }

    override def getExpireInterval()(
        implicit
        backend: SttpBackend[F, Fs2Streams[F]]
    ): F[Int] = getAll().map { kinds =>
      val numOfKinds = kinds.size
      86400 / (999 / numOfKinds) + 1
    }

    override def getPrice(kind: String)(
        implicit
        backend: SttpBackend[F, Fs2Streams[F]]
    ): F[InstancePrice] = retryingOnAllErrors(retryPolicy, logError) {
      basicRequest
        .get(uri"${config.baseUri}/instances/$kind")
        .auth
        .bearer(config.token)
        .response(asJson[InstancePrice])
        .send(backend)
        .map { resp =>
          resp.body.toTry.get
        }
    }

  }

}
