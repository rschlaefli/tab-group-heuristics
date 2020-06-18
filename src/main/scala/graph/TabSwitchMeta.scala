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

  def apply(count: Int = 1): TabSwitchMeta = {
    TabSwitchMeta(count = count, lastUsed = DateTime.now().getMillis())
  }

  def apply(existingMeta: Option[TabSwitchMeta]): Option[TabSwitchMeta] =
    existingMeta match {
      case Some(existing) => Some(TabSwitchMeta(existing.count + 1))
      case None           => Some(TabSwitchMeta())
    }
}
