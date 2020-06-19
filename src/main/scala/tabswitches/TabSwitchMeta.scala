package tabswitches

import org.joda.time.DateTime
import io.circe._, io.circe.generic.semiauto._

case class TabSwitchMeta(
    tab1: TabMeta,
    tab2: TabMeta,
    count: Int,
    lastUsed: Long
)

object TabSwitchMeta {
  implicit val encoder: Encoder[TabSwitchMeta] = deriveEncoder
  implicit val decoder: Decoder[TabSwitchMeta] = deriveDecoder

  def apply(tab1: TabMeta, tab2: TabMeta, count: Int = 1): TabSwitchMeta = {
    TabSwitchMeta(
      tab1 = tab1,
      tab2 = tab2,
      count = count,
      lastUsed = DateTime.now().getMillis()
    )
  }

  def apply(
      existingMeta: Option[TabSwitchMeta],
      tab1: TabMeta,
      tab2: TabMeta
  ): Option[TabSwitchMeta] =
    existingMeta match {
      case Some(existing) => Some(TabSwitchMeta(tab1, tab2, existing.count + 1))
      case None           => Some(TabSwitchMeta(tab1, tab2))
    }
}
