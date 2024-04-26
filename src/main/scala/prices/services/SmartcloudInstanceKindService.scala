package prices.services

import cats.implicits._
import cats.effect._
import org.http4s._
import org.http4s.circe._

import prices.data._
import sttp.client3._
import sttp.client3.http4s._
import sttp.client3.circe._
import io.circe.generic.auto._
import retry._
import retry.RetryDetails._
import scala.concurrent.duration._

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

    def logError(target: String)(err: Throwable, details: RetryDetails): F[Unit] = details match {
      case WillDelayAndRetry(nextDelay: FiniteDuration, retriesSoFar: Int, cumulativeDelay: FiniteDuration) =>
        s"Failed to get $target. So far we have retried $retriesSoFar times.".pure[F].map(println)
      case GivingUp(totalRetries: Int, totalDelay: FiniteDuration) =>
        s"Giving up getting $target after $totalRetries retries. err: $err".pure[F].map(println)
    }

    override def getAll(): F[List[String]] =
      retryingOnAllErrors(retryPolicy, logError("instances")) {
        Http4sBackend
          .usingDefaultEmberClientBuilder()
          .use { backend =>
            basicRequest
              .get(uri"${config.baseUri}/instances")
              .auth
              .bearer(config.token)
              .response(asJson[List[String]])
              .send(backend)
              .map { resp =>
                resp.body.toOption.get
              }
          }
      }

    override def getExpireInterval(): F[Int] =
      getAll().map { kinds =>
        val numOfKinds = kinds.size
        86400 / (999 / numOfKinds) + 1
      }

    override def getPrice(kind: String): F[InstancePrice] =
      retryingOnAllErrors(retryPolicy, logError(s"instances/$kind")) {
        Http4sBackend
          .usingDefaultEmberClientBuilder()
          .use { backend =>
            basicRequest
              .get(uri"${config.baseUri}/instances/$kind")
              .auth
              .bearer(config.token)
              .response(asJson[InstancePrice])
              .send(backend)
              .map { resp =>
                resp.body.toOption.get
              }
          }
      }
  }

}
