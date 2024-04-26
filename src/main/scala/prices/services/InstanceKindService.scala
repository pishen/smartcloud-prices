package prices.services

import scala.util.control.NoStackTrace
import prices.data.InstancePrice

trait InstanceKindService[F[_]] {
  def getAll(): F[List[String]]
  def getExpireInterval(): F[Int]
  def getPrice(kind: String): F[InstancePrice]
}

object InstanceKindService {

  sealed trait Exception extends NoStackTrace
  object Exception {
    case class APICallFailure(message: String) extends Exception
  }

}
