package tabstate

import com.typesafe.scalalogging.LazyLogging
import scala.collection.mutable.{Map, Queue}

import util._

object TabState extends LazyLogging {
  var activeTab = -1
  var activeWindow = -1
  var currentTabs = List[Tab]()
  val tabSwitches = Map[Int, Map[Int, Int]]()

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
        // find the existing tab if it exists
        val existingIndex = currentTabs.indexWhere(_.id == id)

        // build a new tab object from the received tab data
        val tabData = new Tab(
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
          if (existingIndex == -1) {
            // if the tab has never been added before, append it to the state
            currentTabs.appended(tabData)
          } else {
            // if the tab already exists in the state, update it
            currentTabs.updated(existingIndex, tabData)
          }
        }
      }

      case TabActivateEvent(id, windowId, previousTabId) => {
        activeTab = id
        activeWindow = windowId

        logger.info(
          s"> Processing tab switch from $previousTabId to $id in window $windowId"
        )

        // TODO: tab switch heuristic
        if (previousTabId.isDefined) {
          tabSwitches.updateWith(previousTabId.get)((switchMap) => {
            Some(switchMap.getOrElse(Map((id, 1))))
          })
        }

      }

      case TabRemoveEvent(id, windowId) => {
        currentTabs.synchronized {
          currentTabs = currentTabs.filter(_.id != id)
        }
      }
    }
  }
}
