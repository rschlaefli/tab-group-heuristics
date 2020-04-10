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
      case TabCreateEvent(
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
        logger.info(s"Tab state processed a create event for id $id")
        state.appended(
          new Tab(
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
        )
      }

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
        state.updated(
          state.indexWhere(_.id == id),
          new Tab(
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
        )
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
