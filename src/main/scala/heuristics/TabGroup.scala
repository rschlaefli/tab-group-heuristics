package heuristics

import io.circe._, io.circe.parser._, io.circe.generic.semiauto._

import tabstate.Tab
import tabswitches.TabMeta

// a group of tabs
case class TabGroup(
    id: String,
    name: String,
    tabs: List[TabMeta]
)

object TabGroup {
  implicit val tabGroupDecoder: Decoder[TabGroup] = deriveDecoder
  implicit val tabGroupEncoder: Encoder[TabGroup] = deriveEncoder
}
