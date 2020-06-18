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

  var temp: Option[Tab] = None

  /**
    * Process a tab switch event
    *
    * @param prevTab The tab being switched from
    * @param currentTab The tab being switched to
    */
  def processTabSwitch(prevTab: Option[Tab], currentTab: Tab): Unit = {
    if (prevTab.isDefined) {
      val previousTab = prevTab.get

      val irrelevantTabs = List("New Tab", "Tab Groups")
      val switchFromIrrelevantTab = irrelevantTabs.contains(previousTab.title)
      val switchToIrrelevantTab = irrelevantTabs.contains(currentTab.title)

      (switchFromIrrelevantTab, switchToIrrelevantTab) match {
        // process a normal tab switch
        case (false, false) => {
          processTabSwitch(previousTab, currentTab)
          temp = None
        }
        // process a switch to an irrelevant tab
        case (false, true) => {
          temp = Some(previousTab)
        }
        // process a switch from an irrelevant tab
        case (true, false) => {
          if (temp.isDefined) processTabSwitch(temp.get, currentTab)
          temp = None
        }
        case _ =>
          logger.debug(
            s"[TabSwitch] Ignored switch from $previousTab to $currentTab"
          )
      }
    }
  }

  /**
    * Add a new tab switch to the tab switch graph
    *
    * @param prevTab The tab being switched from
    * @param currentTab The tab being switched to
    */
  def processTabSwitch(prevTab: Tab, currentTab: Tab): Unit = {
    // do not track a switch that has the same tab as source and target
    if (prevTab.hash == currentTab.hash) return

    logger.info(
      logToCsv,
      s"${prevTab.id};${prevTab.hash};${prevTab.baseUrl};${prevTab.normalizedTitle};" +
        s"${currentTab.id};${currentTab.hash};${currentTab.baseUrl};${currentTab.normalizedTitle}"
    )

    val List(meta1, meta2) =
      List(prevTab, currentTab)
        .map(TabMeta.apply)
        .sortBy(tabMeta => tabMeta.hash)

    // update the tab switch meta information
    val switchIdentifier = s"${meta1.hash}_${meta2.hash}"
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
