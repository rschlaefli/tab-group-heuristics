package events

import io.circe._, io.circe.generic.semiauto._

case class TabActivateEvent(
    id: Int,
    windowId: Int,
    previousTabId: Option[Int]
) extends TabEvent

object TabActivateEvent {
  implicit val tabActivateEventDecoder: Decoder[TabActivateEvent] =
    deriveDecoder
}
