package tabstate

import com.typesafe.scalalogging.LazyLogging
import scala.collection.mutable.{Map, Queue}

import util._

object TabState extends LazyLogging {
  var activeTab = -1
  var activeWindow = -1
  var currentTabs = Map[Int, Tab]()
  var tabSwitches = Map[String, Map[String, Int]]()
  var tabHashes = Map[String, String]()

  def processQueue(tabEventsQueue: Queue[TabEvent]): Thread = {
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
      case TabUpdateEvent(
          id,
          index,
          windowId,
          active,
          lastAccessed,
          url,
          hash,
          title,
          pinned,
          status,
          baseUrl,
          attention,
          hidden,
          discarded,
          openerTabId,
          sessionId,
          successorTabId
          ) => {
        // build a new tab object from the received tab data
        val tabData = new Tab(
          hash,
          baseUrl,
          active,
          id,
          index,
          lastAccessed,
          openerTabId,
          pinned,
          sessionId,
          successorTabId,
          title,
          url,
          windowId
        )

        currentTabs.synchronized {
          currentTabs.update(id, tabData)
          tabHashes.update(hash, baseUrl)
        }
      }

      case TabActivateEvent(id, windowId, previousTabId) => {
        activeTab = id
        activeWindow = windowId

        logger.info(
          s"> Processing tab switch from $previousTabId to $id in window $windowId"
        )

        // update the map of tab switches based on the new event
        if (currentTabs.contains(id) && previousTabId.isDefined && currentTabs
              .contains(previousTabId.get)) {
          val previousTabHash = currentTabs.get(previousTabId.get).get.hash
          val currentTabHash = currentTabs.get(id).get.hash
          tabSwitches.updateWith(previousTabHash)((switchMap) => {
            val map = switchMap.getOrElse(Map((currentTabHash, 0)))

            map.updateWith(currentTabHash) {
              case Some(value) => Some(value + 1)
              case None        => Some(1)
            }

            logger.info(s"> Updated switch map for tab $previousTabId => $map")

            Some(map)
          })
        }

      }

      case TabRemoveEvent(id, windowId) => {
        currentTabs.synchronized {
          currentTabs -= (id)
        }
      }
    }
  }
}
