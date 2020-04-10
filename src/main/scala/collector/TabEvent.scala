package collector

import argonaut._, Argonaut._

case class TabEventPayload(
    id: Option[Int],
    index: Option[Int],
    windowId: Option[Int],
    active: Option[Boolean],
    attention: Option[Boolean],
    pinned: Option[Boolean],
    status: Option[String],
    hidden: Option[Boolean],
    discarded: Option[Boolean],
    lastAccessed: Option[Int],
    url: Option[String],
    title: Option[String]
)

object TabEventPayload {
  implicit def TabEventPayloadCodecJson: CodecJson[TabEventPayload] =
    casecodec12(TabEventPayload.apply, TabEventPayload.unapply)(
      "id",
      "index",
      "windowId",
      "active",
      "attention",
      "pinned",
      "status",
      "hidden",
      "discarded",
      "lastAccessed",
      "url",
      "title"
    )
}

case class TabEvent(action: String, payload: TabEventPayload)

object TabEvent {
  implicit def TabEventCodecJson: CodecJson[TabEvent] =
    casecodec2(TabEvent.apply, TabEvent.unapply)("action", "payload")
}
