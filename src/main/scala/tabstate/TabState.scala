package tabstate

import com.typesafe.scalalogging.LazyLogging

import events._

object TabState extends LazyLogging {

  def initialize: List[Tab] = {
    val initialState = List[Tab]()
    logger.info(s"Initializing tab state: $initialState")
    initialState
  }

  def processEvent(state: List[Tab], event: TabEvent): List[Tab] = {
    logger.info(s"Processing event $event")
    logger.info(s"Current tab state $state")

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
        logger.info(s"Tab state processed an update event for id $id")
        // find the existing tab if it exists
        val existingIndex = state.indexWhere(_.id == id)
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
          state.appended(tabData)
        } else {
          // if the tab already exists in the state, update it
          state.updated(existingIndex, tabData)
        }
      }

      case TabActivateEvent(id, windowId, previousTabId) => {
        logger.info(s"Tab state processed an activate event for id $id")
        state
      }
      case TabRemoveEvent(id, windowId) => {
        logger.info(s"Tab state processed a remove event for id $id")
        state.filter(_.id != id)
      }
    }
  }

}
