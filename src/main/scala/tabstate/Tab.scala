package tabstate

import io.circe._
import io.circe.generic.semiauto._

// create a trait that all nodes in the tab switch graph will be sharing
sealed trait Tabs

// create a case class based on the tab interface
// augmented with url variations and hashes thereof
// ref: https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions/API/tabs/Tab
case class Tab(
    // Tabs.Tab properties that are important for grouping
    id: Int,
    index: Int,
    lastAccessed: Option[Long],
    openerTabId: Option[Int],
    pinned: Boolean,
    sessionId: Option[Int],
    successorTabId: Option[Int],
    title: String,
    url: String,
    windowId: Int,
    // derived properties
    normalizedTitle: String,
    hash: String,
    origin: String,
    baseUrl: String,
    // internal properties
    createdAt: Long = java.time.Instant.EPOCH.getEpochSecond()
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
  override def toString(): String = s"$normalizedTitle (${hashCode()})"

  def withAccessTs(ts: Long) = this.copy(lastAccessed = Some(ts))
  def withCurrentAccessTs =
    this.copy(lastAccessed = Some(java.time.Instant.EPOCH.getEpochSecond()))
}

object Tab {

  implicit val tabDecoder: Decoder[Tab] = deriveDecoder
  implicit val tabEncoder: Encoder[Tab] = deriveEncoder

  def fromEvent(event: TabUpdateEvent): Tab = Tab(
    event.id,
    event.index,
    event.lastAccessed,
    event.openerTabId,
    event.pinned,
    event.sessionId,
    event.successorTabId,
    event.title,
    event.url,
    event.windowId,
    event.normalizedTitle,
    event.hash,
    event.origin,
    event.baseUrl
  )

}
