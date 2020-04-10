package events

import io.circe._, io.circe.generic.semiauto._

case class TabCreateEvent(
    id: Int,
    index: Int,
    windowId: Int,
    active: Boolean,
    lastAccessed: Double,
    url: String,
    title: String,
    pinned: Boolean,
    status: String,
    attention: Option[Boolean],
    hidden: Option[Boolean],
    discarded: Option[Boolean],
    openerTabId: Option[Int],
    sessionId: Option[Int],
    successorTabId: Option[Int]
) extends TabEvent

object TabCreateEvent {
  implicit val tabCreateEventDecoder: Decoder[TabCreateEvent] =
    deriveDecoder
}
