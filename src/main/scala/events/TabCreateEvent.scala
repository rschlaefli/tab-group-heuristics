package events

import io.circe._, io.circe.generic.semiauto._

case class TabCreateEvent(
    id: Option[Int],
    index: Option[Int],
    windowId: Option[Int],
    active: Option[Boolean],
    attention: Option[Boolean],
    pinned: Option[Boolean],
    status: Option[String],
    hidden: Option[Boolean],
    discarded: Option[Boolean],
    lastAccessed: Option[Double],
    url: Option[String],
    title: Option[String],
    openerTabId: Option[Int],
    sessionId: Option[Int],
    successorTabId: Option[Int]
) extends TabEvent

object TabCreateEvent {
  implicit val tabCreateEventDecoder: Decoder[TabCreateEvent] =
    deriveDecoder
}
