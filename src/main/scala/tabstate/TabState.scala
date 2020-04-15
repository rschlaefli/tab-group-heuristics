package tabstate

import com.typesafe.scalalogging.LazyLogging
import scala.collection.mutable.{Map, Queue}
import scalax.collection.Graph
import scalax.collection.edge.WDiEdge

import messaging._
import heuristics.TabSwitches
import util._

object TabState extends LazyLogging {
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
        currentTabs ++= initialTabs.map(tab => (tab.id, tab))

        TabSwitches.processInitialTabs(initialTabs)

        logger.info(
          s"> Initialized current tabs to $currentTabs"
        )
      }

      case updateEvent: TabUpdateEvent => {
        // build a new tab object from the received tab data
        val tab = Tab.fromEvent(updateEvent)

        currentTabs.synchronized {
          TabSwitches.processTabSwitch(currentTabs.get(tab.id), tab)
          currentTabs.update(tab.id, tab)

        }
      }

      case TabActivateEvent(id, windowId, previousTabId) => {
        activeTab = id
        activeWindow = windowId

        logger.info(
          s"> Processing switch from $previousTabId to $id in window $windowId"
        )

        // update the map of tab switches based on the new event
        if (currentTabs.contains(id) && previousTabId.isDefined) {
          val previousTab = currentTabs.get(previousTabId.get)
          val currentTab = currentTabs.get(id).get
          TabSwitches.processTabSwitch(previousTab, currentTab)
        }

      }

      case TabRemoveEvent(id, windowId) => {
        currentTabs.synchronized {
          currentTabs -= (id)
        }
      }

      case _: TabEvent => logger.warn("Received unknown TabEvent")
    }
  }
}
