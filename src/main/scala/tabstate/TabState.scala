package tabstate

import com.typesafe.scalalogging.LazyLogging
import collector.Utils
import scala.collection.mutable

import events._

object TabState extends LazyLogging {
  var currentTabs = List[Tab]()

  def processQueue(tabEventsQueue: mutable.Queue[TabEvent]) =
    new Thread(() =>
      Iterator
        .continually(Utils.dequeueTabEvent(tabEventsQueue))
        .foreach(tabEvent => currentTabs = processEvent(tabEvent))
    )

  def processEvent(event: TabEvent): List[Tab] = {
    logger.info(s"Processing event $event")

    currentTabs = event match {
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
        logger.info(s"Tab state processed an update event for id $id")
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

        if (existingIndex == -1) {
          // if the tab has never been added before, append it to the state
          currentTabs.appended(tabData)
        } else {
          // if the tab already exists in the state, update it
          currentTabs.updated(existingIndex, tabData)
        }
      }

      case TabActivateEvent(id, windowId, previousTabId) => {
        logger.info(
          s"Tab state processed an activate event for id $id with previousId $previousTabId"
        )
        currentTabs
      }
      case TabRemoveEvent(id, windowId) => {
        logger.info(s"Tab state processed a remove event for id $id")
        currentTabs.filter(_.id != id)
      }
    }
  }
}
