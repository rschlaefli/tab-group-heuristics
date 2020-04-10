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
    logger.info(s"Processing event $event with state $state")

    event match {
      case TabCreateEvent(
          id,
          index,
          windowId,
          active,
          attention,
          pinned,
          status,
          hidden,
          discarded,
          lastAccessed,
          url,
          title,
          openerTabId,
          sessionId,
          successorId
          ) => {
        logger.info(s"Tab state processed a create event for id $id")
      }
      case TabUpdateEvent(
          id,
          index,
          windowId,
          active,
          attention,
          pinned,
          status,
          hidden,
          discarded,
          lastAccessed,
          url,
          title,
          openerTabId,
          sessionId,
          successorId
          ) => {
        logger.info(s"Tab state processed an update event for id $id")
      }
      case TabActivateEvent(id, windowId, previousTabId) => {
        logger.info(s"Tab state processed an activate event for id $id")
      }
      case TabRemoveEvent(id, windowId) => {
        logger.info(s"Tab state processed a remove event for id $id")
      }
    }

    state
  }

}
