package tabswitches

import io.circe._
import io.circe.generic.semiauto._
import org.joda.time.DateTime
import java.net.URL
import org.simmetrics.metrics.StringMetrics
import scala.util.Try
import scala.util.Success

case class TabSwitchMeta(
    tab1: TabMeta,
    tab2: TabMeta,
    count: Int,
    lastUsed: Long,
    sameOrigin: Option[Boolean],
    urlSimilarity: Option[Float]
)

object TabSwitchMeta {
  implicit val encoder: Encoder[TabSwitchMeta] = deriveEncoder
  implicit val decoder: Decoder[TabSwitchMeta] = deriveDecoder

  def apply(
      tab1: TabMeta,
      tab2: TabMeta,
      count: Int = 1,
      lastUsed: Option[Long] = None
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
      case _ => (None, None)
    }

    TabSwitchMeta(
      tab1 = tab1,
      tab2 = tab2,
      count = count,
      lastUsed = lastUsed getOrElse DateTime.now().getMillis(),
      sameOrigin = sameOrigin,
      urlSimilarity = urlSimilarity
    )
  }

  def apply(
      existingMeta: Option[TabSwitchMeta],
      tab1: TabMeta,
      tab2: TabMeta
  ): Option[TabSwitchMeta] =
    existingMeta match {
      case Some(existing) => Some(TabSwitchMeta(tab1, tab2, existing.count + 1))
      case None           => Some(TabSwitchMeta(tab1, tab2))
    }

  def clone(existingMeta: TabSwitchMeta): TabSwitchMeta = {
    TabSwitchMeta(
      tab1 = existingMeta.tab1,
      tab2 = existingMeta.tab2,
      count = existingMeta.count,
      lastUsed = Some(existingMeta.lastUsed)
    )
  }
}
