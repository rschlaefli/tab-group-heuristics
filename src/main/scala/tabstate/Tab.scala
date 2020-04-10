package tabstate

// see https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions/API/tabs/Tab
case class Tab(
    active: Boolean,
    highlighted: Boolean,
    id: Int,
    index: Int,
    lastAccessed: Double,
    openerTabId: Option[Int],
    pinned: Boolean,
    sessionId: Option[Int],
    successorId: Option[Int],
    title: String,
    url: String,
    windowId: Int
)
