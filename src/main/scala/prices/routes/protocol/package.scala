package prices.routes

import org.http4s.QueryParamDecoder
import org.http4s.dsl.impl.QueryParamDecoderMatcher

import prices.data._

package object protocol {

  implicit val instanceKindQueryParam: QueryParamDecoder[InstanceKind] =
    QueryParamDecoder[String].map(InstanceKind.apply)

  object InstanceKindQueryParam extends QueryParamDecoderMatcher[InstanceKind]("instanceKind")

}
