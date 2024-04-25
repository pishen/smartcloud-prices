package prices.routes

import cats.implicits._
import cats.effect._
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

import prices.services.InstanceKindService
import cats.effect.std.AtomicCell
import prices.data.InstancePrice
import java.time._

final case class InstanceKindRoutes[F[_]: Sync](instanceKindService: InstanceKindService[F], prices: AtomicCell[F, Map[String, InstancePrice]], expireInterval: Int) extends Http4sDsl[F] {

  implicit val instanceKindResponseEncoder = jsonEncoderOf[F, List[String]]

  object Kind extends QueryParamDecoderMatcher[String]("kind")

  def isExpire(ip: InstancePrice): Boolean = {
    val now = Instant.now.getEpochSecond
    val priceTime = ZonedDateTime.parse(ip.timestamp).toInstant.getEpochSecond
    now - priceTime > expireInterval
  }

  def tryUpdate(kind: String): F[Map[String, InstancePrice]] = {
    prices.evalUpdateAndGet { priceMap =>
      priceMap.get(kind).filterNot(isExpire) match {
        case Some(_) => priceMap.pure[F]
        case None => instanceKindService.getPrice(kind).map(ip => priceMap + (kind -> ip))
      }
    }
  }

  private val get: HttpRoutes[F] = HttpRoutes.of {
    case GET -> Root / "instance-kinds" =>
      instanceKindService.getAll().flatMap(kinds => Ok(kinds))
    case GET -> Root / "prices" :? Kind(kind) =>
      val price = for {
        priceMap <- prices.get
        updatedPriceMap <- priceMap.get(kind) match {
          case Some(ip) =>
            if (isExpire(ip)) tryUpdate(kind) else priceMap.pure[F]
          case None => tryUpdate(kind)
        }
      } yield {
        println("updatedPriceMap: " + updatedPriceMap)
        updatedPriceMap.get(kind).map(_.price.toString).getOrElse("N/A")
      }
      Ok(price)
  }

  def routes: HttpRoutes[F] = Router("/" -> get)

}
