package tabstate

import io.circe._, io.circe.parser._, io.circe.generic.semiauto._

// see https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions/API/tabs/Tab
case class Tab(
    hash: String,
    baseUrl: String,
    active: Boolean,
    // highlighted: Boolean,
    id: Int,
    index: Int,
    lastAccessed: Double,
    openerTabId: Option[Int],
    pinned: Boolean,
    sessionId: Option[Int],
    successorTabId: Option[Int],
    title: String,
    url: String,
    windowId: Int
)

object Tab {
  implicit val tabDecoder: Decoder[Tab] = deriveDecoder
}

// a group of tabs
case class TabGroup(
    id: String,
    name: String,
    tabs: List[Tab]
)
