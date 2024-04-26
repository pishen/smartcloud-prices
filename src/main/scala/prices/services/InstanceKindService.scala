package prices.services

import scala.util.control.NoStackTrace

import prices.data._
import sttp.client3.SttpBackend
import sttp.capabilities.fs2.Fs2Streams

trait InstanceKindService[F[_]] {
  def getAll()(
      implicit
      backend: SttpBackend[F, Fs2Streams[F]]
  ): F[List[InstanceKind]]
}

object InstanceKindService {

  sealed trait Exception extends NoStackTrace
  object Exception {
    case class APICallFailure(message: String) extends Exception
  }

}
