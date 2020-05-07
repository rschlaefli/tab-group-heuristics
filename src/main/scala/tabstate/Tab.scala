package tabstate

import io.circe._, io.circe.parser._, io.circe.generic.semiauto._

import messaging.TabUpdateEvent

// create a trait that all nodes in the tab switch graph will be sharing
sealed trait Tabs

// create a case class based on the tab interface
// augmented with url variations and hashes thereof
// ref: https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions/API/tabs/Tab
case class Tab(
    hash: String,
    origin: String,
    originHash: String,
    baseHash: String,
    baseUrl: String,
    active: Boolean,
    // highlighted: Boolean,
    id: Int,
    index: Int,
    // lastAccessed: Double,
    openerTabId: Option[Int],
    pinned: Boolean,
    sessionId: Option[Int],
    successorTabId: Option[Int],
    title: String,
    url: String,
    windowId: Int
) extends Tabs {

  // override canEqual, equals, and hashCode to ensure that tabs are compared by base hash
  // ref: https://alvinalexander.com/scala/how-to-define-equals-hashcode-methods-in-scala-object-equality

  override def canEqual(a: Any) = a.isInstanceOf[Tab]

  override def equals(that: Any): Boolean = {
    that match {
      case that: Tab => that.canEqual(this) && this.hash == that.hash
      case _         => false
    }
  }

  override def hashCode(): Int = hash.hashCode()

  // override toString to reduce clutter in graph representations
  override def toString(): String = s"$title (${hashCode()})"
}

object Tab {

  implicit val tabDecoder: Decoder[Tab] = deriveDecoder

  def fromEvent(event: TabUpdateEvent): Tab = new Tab(
    event.hash,
    event.origin,
    event.originHash,
    event.baseHash,
    event.baseUrl,
    event.active,
    event.id,
    event.index,
    // event.lastAccessed,
    event.openerTabId,
    event.pinned,
    event.sessionId,
    event.successorTabId,
    event.title,
    event.url,
    event.windowId
  )

}
