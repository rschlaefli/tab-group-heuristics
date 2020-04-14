package tabstate

import com.typesafe.scalalogging.LazyLogging
import scala.collection.mutable.{Map, Queue}

import util._

object TabState extends LazyLogging {
  var activeTab = -1
  var activeWindow = -1

  var currentTabs = Map[Int, Tab]()

  var tabBaseSwitches = Map[String, Map[String, Int]]()
  var tabOriginSwitches = Map[String, Map[String, Int]]()
  var tabBaseHashes = Map[String, String]()
  var tabOriginHashes = Map[String, String]()

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
        tabBaseHashes ++= initialTabs.map(tab => (tab.baseHash, tab.baseUrl))
        tabOriginHashes ++= initialTabs.map(tab => (tab.originHash, tab.origin))
        logger.info(s"> Initialized current tabs to $currentTabs")
      }

      case TabUpdateEvent(
          id,
          index,
          windowId,
          active,
          lastAccessed,
          url,
          baseHash,
          baseUrl,
          origin,
          originHash,
          title,
          pinned,
          status,
          attention,
          hidden,
          discarded,
          openerTabId,
          sessionId,
          successorTabId
          ) => {
        // build a new tab object from the received tab data
        val tabData = new Tab(
          origin,
          originHash,
          baseHash,
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

          tabBaseHashes.update(baseHash, baseUrl)
          tabOriginHashes.update(originHash, origin)
        }
      }

      case TabActivateEvent(id, windowId, previousTabId) => {
        activeTab = id
        activeWindow = windowId

        logger.info(
          s"> Processing switch from $previousTabId to $id in window $windowId"
        )

        // update the map of tab switches based on the new event
        if (currentTabs.contains(id) && previousTabId.isDefined && currentTabs
              .contains(previousTabId.get)) {
          val previousTab = currentTabs.get(previousTabId.get).get
          val currentTab = currentTabs.get(id).get

          tabBaseSwitches.updateWith(previousTab.baseHash)((switchMap) => {
            val map = switchMap.getOrElse(Map((currentTab.baseHash, 0)))

            map.updateWith(currentTab.baseHash) {
              case Some(value) => Some(value + 1)
              case None        => Some(1)
            }

            logger.debug(s"> Updated switch map for tab $previousTabId => $map")

            Some(map)
          })

          tabOriginSwitches.updateWith(currentTab.originHash)((switchMap) => {
            val map = switchMap.getOrElse(Map((currentTab.originHash, 0)))

            map.updateWith(currentTab.originHash) {
              case Some(value) => Some(value + 1)
              case None        => Some(1)
            }

            logger.debug(s"> Updated switch map for tab $previousTabId => $map")

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
