package tabstate

import com.typesafe.scalalogging.LazyLogging
import collector.TabEvent

object TabState extends LazyLogging {

  def initialize: List[Tab] = {
    val initialState = List[Tab]()
    logger.info(s"Initializing tab state: $initialState")
    initialState
  }

  def processEvent(state: List[Tab], event: TabEvent): List[Tab] = {
    logger.info(s"Processing event: $event")
    logger.info(s"Computed new state: $state")

    if (event.action == "CREATE") {
      // event.payload
      // return state.appended()
    }

    state
  }

}
