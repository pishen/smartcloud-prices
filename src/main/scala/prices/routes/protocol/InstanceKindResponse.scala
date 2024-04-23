package prices.routes.protocol

import io.circe._
import io.circe.syntax._

import prices.data._

final case class InstanceKindResponse(value: InstanceKind)

object InstanceKindResponse {

  implicit val encoder: Encoder[InstanceKindResponse] =
    Encoder.instance[InstanceKindResponse] {
      case InstanceKindResponse(k) =>
        Json.obj(
          "kind" -> k.getString.asJson
        )
    }

}
