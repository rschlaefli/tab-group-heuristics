package tabswitches

import io.circe._
import io.circe.generic.semiauto._
import tabstate.Tab

case class TabMeta(
    hash: String,
    title: String,
    url: String
)

object TabMeta {
  implicit val encoder: Encoder[TabMeta] = deriveEncoder
  implicit val decoder: Decoder[TabMeta] = deriveDecoder

  def apply(tab: Tab): TabMeta =
    TabMeta(hash = tab.hash, title = tab.normalizedTitle, url = tab.baseUrl)
}
