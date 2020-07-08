package statistics

import io.circe._
import io.circe.generic.semiauto._

case class UsageStatistics(activeMinutes: Int = 0)

object UsageStatistics {
  implicit val usageDecoder: Decoder[UsageStatistics] = deriveDecoder
  implicit val usageEncoder: Encoder[UsageStatistics] = deriveEncoder
}
