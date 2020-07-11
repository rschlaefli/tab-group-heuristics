package tabswitches

import io.circe._
import io.circe.generic.semiauto._
import tabstate.Tab

trait TabInfo

case class TabMeta(
    hash: String,
    title: String,
    url: String,
    pageRank: Option[Double] = None
) extends TabInfo {

  def withPageRank(pageRank: Double): TabMeta =
    TabMeta(hash, title, url, Some(pageRank))

}

object TabMeta {
  implicit val encoder: Encoder[TabMeta] = deriveEncoder
  implicit val decoder: Decoder[TabMeta] = deriveDecoder

  def apply(tab: Tab): TabMeta =
    TabMeta(hash = tab.hash, title = tab.normalizedTitle, url = tab.baseUrl)

}
