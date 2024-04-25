package prices.data

final case class InstanceKind(getString: String) extends AnyVal

case class InstancePrice(kind: String, price: Double, timestamp: String)
