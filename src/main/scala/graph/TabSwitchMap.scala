package graph

import scala.collection.{mutable}
import org.joda.time.DateTime
import scala.util.Try
import io.circe._, io.circe.parser._, io.circe.generic.semiauto._,
io.circe.syntax._
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.scalalogging.LazyLogging
import org.slf4j.MarkerFactory
import scala.concurrent.Future

import tabstate.Tab
import persistence.Persistable
import statistics.StatisticsEngine

object TabSwitchMap extends LazyLogging with Persistable {
  val logToCsv = MarkerFactory.getMarker("CSV")

  var tabSwitches = mutable.Map[String, TabSwitchMeta]()

  /**
    * Add a new tab switch to the tab switch graph
    *
    * @param prevTab The tab being switched from
    * @param currentTab The tab being switched to
    */
  def processTabSwitch(prevTab: Tab, currentTab: Tab) = {
    logger.info(
      logToCsv,
      s"${prevTab.id};${prevTab.hash};${prevTab.baseUrl};${prevTab.normalizedTitle};${currentTab.id};${currentTab.hash};${currentTab.baseUrl};${currentTab.normalizedTitle}"
    )

    val List(meta1, meta2) =
      List(prevTab, currentTab)
        .map(TabMeta.apply)
        .sortBy(tabMeta => tabMeta.hash)

    val switchIdentifier = s"${meta1.hash}_${meta2.hash}"

    // update the tab switch meta information
    tabSwitches.updateWith(switchIdentifier)(TabSwitchMeta(_, meta1, meta2))

    // add the tab switch to the statistics switch queue
    Future {
      StatisticsEngine.tabSwitchQueue.synchronized {
        StatisticsEngine.tabSwitchQueue.enqueue((prevTab, currentTab))
      }
    }
  }

  override def persist: Try[Unit] = Try {
    Persistable
      .persistJson("tab_switches.json", tabSwitches.asJson)
  }

  override def restore: Try[Unit] = Try {
    Persistable
      .restoreJson("tab_switches.json")
      .map(decode[mutable.Map[String, TabSwitchMeta]])
      .foreach {
        case Right(restoredMap) => tabSwitches = restoredMap
      }
  }
}
