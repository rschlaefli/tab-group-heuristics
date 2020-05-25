package tabstate

import com.typesafe.scalalogging.LazyLogging
import scala.collection.mutable.{Map, Queue}
import scalax.collection.Graph
import scalax.collection.edge.WDiEdge
import org.slf4j.MarkerFactory

import messaging._
import heuristics.TabSwitches
import util.Utils

object TabState extends LazyLogging {
  val logToCsv = MarkerFactory.getMarker("CSV")

  var activeTab = -1
  var activeWindow = -1

  var currentTabs = Map[Int, Tab]()

  def apply(tabEventsQueue: Queue[TabEvent]): Thread = {

    val thread = new Thread(() => {
      logger.info("> Starting to process tab events")

      Iterator
        .continually(dequeueTabEvent(tabEventsQueue))
        .foreach(processEvent)
    })

    thread.setName("TabState")
    thread.setDaemon(true)
    thread
  }

  def dequeueTabEvent[T](queue: Queue[T]): T = {
    queue.synchronized {
      if (queue.isEmpty) {
        queue.wait()
      }
      return queue.dequeue()
    }
  }

  def processEvent(event: TabEvent): Unit = {
    logger.debug(s"> Processing tab event $event")

    event match {
      case TabInitializationEvent(initialTabs) => {
        currentTabs ++= initialTabs.map(tab => {
          logger.info(
            logToCsv,
            s"UPDATE;${tab.id};${tab.hash};${tab.baseUrl};${tab.normalizedTitle};;;;"
          )
          (tab.id, tab)
        })

        TabSwitches.processInitialTabs(initialTabs)

        logger.info(
          s"> Initialized current tabs to $currentTabs"
        )
      }

      case updateEvent: TabUpdateEvent => {
        // build a new tab object from the received tab data
        val tab = Tab.fromEvent(updateEvent)

        logger.info(
          logToCsv,
          s"UPDATE;${tab.id};${tab.hash};${tab.baseUrl};${tab.normalizedTitle};;;;"
        )

        currentTabs.synchronized {
          TabSwitches.processTabSwitch(currentTabs.get(tab.id), tab)
          currentTabs.update(tab.id, tab)
        }
      }

      case TabActivateEvent(id, windowId, previousTabId) => {
        activeTab = id
        activeWindow = windowId

        logger.debug(
          s"> Processing switch from $previousTabId to $id in window $windowId"
        )

        // update the map of tab switches based on the new event
        if (currentTabs.contains(id) && previousTabId.isDefined) {
          val previousTab = currentTabs.get(previousTabId.get)
          val currentTab = currentTabs.get(id).get

          val csvRow = previousTab match {
            case Some(tab) =>
              s"SWITCH;${tab.id};${tab.hash};${tab.baseUrl};${tab.normalizedTitle};${currentTab.id};${currentTab.hash};${currentTab.baseUrl};${currentTab.normalizedTitle}"
            case None =>
              s"SWITCH;;;;;${currentTab.id};${currentTab.hash};${currentTab.baseUrl};${currentTab.normalizedTitle}"
          }

          logger.info(logToCsv, csvRow)

          TabSwitches.processTabSwitch(previousTab, currentTab)
        }

      }

      case TabRemoveEvent(id, windowId) => {
        logger.info(logToCsv, s"REMOVE;${id};;;;;;;")

        currentTabs.synchronized {
          currentTabs -= (id)
        }
      }

      case _: TabEvent => logger.warn("Received unknown TabEvent")
    }
  }
}
