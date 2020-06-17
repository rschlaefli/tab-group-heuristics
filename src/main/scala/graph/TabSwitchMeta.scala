package graph

import org.joda.time.DateTime
import io.circe._, io.circe.generic.semiauto._

case class TabSwitchMeta(
    count: Int,
    lastUsed: Long
)

object TabSwitchMeta {
  implicit val encoder: Encoder[TabSwitchMeta] = deriveEncoder
  implicit val decoder: Decoder[TabSwitchMeta] = deriveDecoder
}
