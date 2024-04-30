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
import io.circe.syntax._
import io.circe.generic.auto._
import cats.data.Validated
import prices.data.InstancePriceWithTime
import prices.data.InstancePrice
import java.time._
import cats.Parallel

final case class InstanceKindRoutes[F[_]: Sync: Parallel](
    instanceKindService: InstanceKindService[F],
    cachedPrices: AtomicCell[F, Map[String, InstancePriceWithTime]],
    expireInterval: Int
)(
    implicit
    backend: SttpBackend[F, Fs2Streams[F]]
) extends Http4sDsl[F] {

  implicit val instanceKindResponseEncoder = jsonEncoderOf[F, List[InstanceKindResponse]]

  def isExpire(price: InstancePriceWithTime): Boolean = {
    val now       = Instant.now.getEpochSecond
    val priceTime = ZonedDateTime.parse(price.timestamp).toInstant.getEpochSecond
    now - priceTime >= expireInterval
  }

  def tryUpdate(kind: String): F[InstancePriceWithTime] =
    cachedPrices.evalModify { prices =>
      prices.get(kind).filterNot(isExpire) match {
        case Some(price) =>
          println(s"Already updated. $prices")
          (prices, price).pure[F]
        case None =>
          instanceKindService.getPrice(kind).map { price =>
            val updatedPrices = prices + (kind -> price)
            println(s"Prices updated: $updatedPrices")
            (updatedPrices, price)
          }
      }
    }

  object Kinds extends OptionalMultiQueryParamDecoderMatcher[String]("kind")

  private val get: HttpRoutes[F] = HttpRoutes.of {
    case GET -> Root / "instance-kinds" =>
      instanceKindService.getAll().flatMap(kinds => Ok(kinds.map(k => InstanceKindResponse(k))))
    case GET -> Root / "prices" :? Kinds(Validated.Valid(Seq(firstKind, others @ _*))) =>
      val kinds = firstKind +: others
      val res = for {
        prices <- cachedPrices.get
        updatedPrices <- kinds.parTraverse { kind =>
                           prices.get(kind) match {
                             case Some(price) =>
                               if (isExpire(price)) tryUpdate(kind) else price.pure[F]
                             case None => tryUpdate(kind)
                           }
                         }
      } yield updatedPrices.map(price => InstancePrice(price.kind, price.price).asJson.noSpaces).mkString(",")
      Ok(res)
  }

  def routes: HttpRoutes[F] = Router("/" -> get)

}
