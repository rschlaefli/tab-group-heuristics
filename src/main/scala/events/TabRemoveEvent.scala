package events

import io.circe._, io.circe.generic.semiauto._

case class TabRemoveEvent(
    id: Int,
    windowId: Int
) extends TabEvent

object TabRemoveEvent {
  implicit val tabRemoveEventDecoder: Decoder[TabRemoveEvent] =
    deriveDecoder
}
