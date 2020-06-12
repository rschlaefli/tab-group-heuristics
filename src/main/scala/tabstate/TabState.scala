package tabstate

import com.typesafe.scalalogging.LazyLogging
import scala.collection.mutable.{Map, Queue}
import scalax.collection.Graph
import scalax.collection.edge.WDiEdge
import org.slf4j.MarkerFactory
import io.circe.Json
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import messaging._
import heuristics._
import util.Utils

object TabState extends LazyLogging {
  val logToCsv = MarkerFactory.getMarker("CSV")

  var activeTab = -1
  var activeWindow = -1

  var currentTabs = Map[Int, Tab]()

  var tabEventsQueue: Queue[TabEvent] = new Queue[TabEvent](20)

  def apply(): Thread = {

    // query the webextension for the list of current tabs
    NativeMessaging.writeNativeMessage(IO.out, HeuristicsAction.QUERY_TABS)

    // query the webextension for the list of current groups
    NativeMessaging.writeNativeMessage(IO.out, HeuristicsAction.QUERY_GROUPS)

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
            s"UPDATE;${tab.id};${tab.hash};${tab.baseUrl};${tab.normalizedTitle}"
          )
          (tab.id, tab)
        })

        TabSwitches.processInitialTabs(initialTabs)

        logger.info(
          s"> Initialized current tabs to $currentTabs"
        )
      }

      case TabGroupUpdateEvent(tabGroups) => {
        logger.debug(
          s"> Updating tab groups to $tabGroups"
        )

        logger.info(logToCsv, s"UPDATE_GROUPS;;;;")

        HeuristicsEngine.updateManualClusters(tabGroups)
      }

      case updateEvent: TabUpdateEvent => {
        // build a new tab object from the received tab data
        val tab = Tab.fromEvent(updateEvent)

        Future {
          currentTabs.synchronized {
            val prevTabState = currentTabs.get(tab.id)

            logger.debug(s"Updating tab from $prevTabState to $tab")

            logger.info(
              logToCsv,
              s"UPDATE;${tab.id};${tab.hash};${tab.baseUrl};${tab.normalizedTitle}"
            )

            currentTabs.update(tab.id, tab)

            if (prevTabState.isDefined) {
              TabSwitches.processTabSwitch(prevTabState, tab)
            }
          }
        }
      }

      case activateEvent: TabActivateEvent => {
        val TabActivateEvent(id, windowId, previousTabId) = activateEvent

        // update the map of tab switches based on the new event
        currentTabs.synchronized {
          val previousTab = currentTabs.get(previousTabId.getOrElse(activeTab))
          val currentTab = currentTabs.get(id)

          logger.debug(
            s"> Processing switch from $previousTab to $currentTab in window $windowId"
          )

          if (!currentTab.isDefined) {
            Future {
              Thread.sleep(333)
              logger.debug(
                "> Tab switch to non-existent tab, pushing back to queue..."
              )
              tabEventsQueue.synchronized {
                tabEventsQueue.enqueue(activateEvent)
                tabEventsQueue.notifyAll()
              }
            }
          } else {
            activeTab = id
            activeWindow = windowId

            logger.info(
              logToCsv,
              s"ACTIVATE;${currentTab.get.id};${currentTab.get.hash};${currentTab.get.baseUrl};${currentTab.get.normalizedTitle}"
            )

            TabSwitches.processTabSwitch(previousTab, currentTab.get)
          }
        }

      }

      case TabRemoveEvent(id, windowId) => {
        logger.info(logToCsv, s"REMOVE;${id};;;")

        currentTabs.synchronized {
          currentTabs -= (id)
        }
      }

      case _: TabEvent => logger.warn("Received unknown TabEvent")
    }
  }
}
