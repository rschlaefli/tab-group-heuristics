package tabstate

import com.typesafe.scalalogging.LazyLogging
import collector.TabEvent

object TabState extends LazyLogging {

  def initialize: Map[String, TabGroup] = {
    val initialState =
      Map[String, TabGroup](
        ("ungrouped", new TabGroup("ungrouped", "Ungrouped", List[Tab]()))
      )
    logger.info(s"Initializing tab state: $initialState")
    initialState
  }

  def processEvent(state: List[TabGroup], event: TabEvent) = {
    logger.info(s"Processing event: $event")
    logger.info(s"Computed new state: $state")
    state
  }

}
