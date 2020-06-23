package heuristics

import io.circe._, io.circe.generic.semiauto._

import tabswitches.TabMeta

case class TabGroup(
    id: String,
    name: String,
    tabs: List[TabMeta]
) {
  def asTuple: (String, Set[TabMeta]) = {
    (name, tabs.toSet)
  }
}

object TabGroup {
  implicit val tabGroupDecoder: Decoder[TabGroup] = deriveDecoder
  implicit val tabGroupEncoder: Encoder[TabGroup] = deriveEncoder
}
