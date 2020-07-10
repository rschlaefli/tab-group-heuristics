package tabswitches

import java.net.URL

import scala.util.Success
import scala.util.Try

import io.circe._
import io.circe.generic.semiauto._
import org.joda.time.DateTime
import org.simmetrics.metrics.StringMetrics

case class TabSwitchMeta(
    tab1: TabMeta,
    tab2: TabMeta,
    count: Int,
    lastUsed: Long,
    sameOrigin: Option[Boolean],
    urlSimilarity: Option[Float],
    wasDiscarded: Option[Boolean]
) {
  def discarded = this.copy(wasDiscarded = Some(true))
}

object TabSwitchMeta {
  implicit val encoder: Encoder[TabSwitchMeta] = deriveEncoder
  implicit val decoder: Decoder[TabSwitchMeta] = deriveDecoder

  def apply(
      tab1: TabMeta,
      tab2: TabMeta,
      count: Int = 1,
      lastUsed: Option[Long] = None,
      wasDiscarded: Option[Boolean] = Some(false)
  ): TabSwitchMeta = {

    val (sameOrigin, urlSimilarity) = Try {
      (new URL(tab1.url), new URL(tab2.url))
    } match {
      case Success((url1, url2)) => {
        val hostPath1 = url1.getHost() + url1.getPath()
        val hostPath2 = url2.getHost() + url2.getPath()

        val metric = StringMetrics.levenshtein()

        (
          Some(url1.getHost() == url2.getHost()),
          Some(metric.compare(hostPath1, hostPath2))
        )
      }
      case _ if tab1.url == tab2.url => (Some(true), Some(1f))
      case _                         => (None, None)
    }

    TabSwitchMeta(
      tab1 = tab1,
      tab2 = tab2,
      count = count,
      lastUsed = lastUsed getOrElse DateTime.now().getMillis(),
      sameOrigin = sameOrigin,
      urlSimilarity = urlSimilarity,
      wasDiscarded = wasDiscarded
    )
  }

  def apply(
      existingMeta: Option[TabSwitchMeta],
      tab1: TabMeta,
      tab2: TabMeta
  ): Option[TabSwitchMeta] =
    existingMeta match {
      case Some(existing) =>
        Some(
          TabSwitchMeta(
            tab1,
            tab2,
            count = existing.count + 1,
            wasDiscarded = existing.wasDiscarded
          )
        )
      case None => Some(TabSwitchMeta(tab1, tab2))
    }

  def clone(existingMeta: TabSwitchMeta): TabSwitchMeta = {
    TabSwitchMeta(
      tab1 = existingMeta.tab1,
      tab2 = existingMeta.tab2,
      count = existingMeta.count,
      lastUsed = Some(existingMeta.lastUsed),
      wasDiscarded = existingMeta.wasDiscarded
    )
  }
}
