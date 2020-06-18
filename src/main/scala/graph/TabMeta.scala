package graph

import org.joda.time.DateTime
import io.circe._, io.circe.generic.semiauto._
import tabstate.Tab

case class TabMeta(
    title: String,
    url: String
)

object TabMeta {
  implicit val encoder: Encoder[TabMeta] = deriveEncoder
  implicit val decoder: Decoder[TabMeta] = deriveDecoder

  def apply(tab: Tab): TabMeta =
    TabMeta(title = tab.normalizedTitle, url = tab.baseUrl)
}
