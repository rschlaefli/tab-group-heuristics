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

  var tabHashes = mutable.Map[String, TabMeta]()
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

    // persist the tab hash mapping for the tab
    tabHashes.update(prevTab.hash, TabMeta(prevTab))
    tabHashes.update(currentTab.hash, TabMeta(currentTab))

    // order prevTab and currentTab hash so that we only get one identifier per pair
    val List(hash1, hash2) = List(prevTab.hash, currentTab.hash).sorted
    val switchIdentifier = s"${hash1}_${hash2}"

    // update the tab switch meta information
    tabSwitches.updateWith(switchIdentifier)(TabSwitchMeta.apply)

    // add the tab switch to the statistics switch queue
    Future {
      StatisticsEngine.tabSwitchQueue.synchronized {
        StatisticsEngine.tabSwitchQueue.enqueue((prevTab, currentTab))
      }
    }
  }

  override def persist: Try[Unit] = Try {
    Persistable
      .persistJson("tab_hashes.json", tabHashes.asJson)

    Persistable
      .persistJson("tab_switches.json", tabSwitches.asJson)
  }

  override def restore: Try[Unit] = Try {
    Persistable
      .restoreJson("tab_hashes.json")
      .map(decode[mutable.Map[String, TabMeta]])
      .foreach {
        case Right(restoredMap) => tabHashes = restoredMap
      }

    Persistable
      .restoreJson("tab_switches.json")
      .map(decode[mutable.Map[String, TabSwitchMeta]])
      .foreach {
        case Right(restoredMap) => tabSwitches = restoredMap
      }
  }
}
