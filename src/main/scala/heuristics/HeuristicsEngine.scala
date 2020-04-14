package heuristics

import com.typesafe.scalalogging.LazyLogging

import tabstate.{TabState}

object HeuristicsEngine extends LazyLogging {
  def apply(): Thread = {
    val thread = new Thread {
      logger.info("> Starting to observe current tab state")
      while (true) {
        logger.debug(
          s"> Current tab state: ${TabState.activeTab}, ${TabState.activeWindow}, ${TabState.currentTabs}, ${TabState.tabOriginGraph}"
        )
        Thread.sleep(30000)
      }
    }

    thread.setName("HeuristicsEngine")

    thread
  }
}
