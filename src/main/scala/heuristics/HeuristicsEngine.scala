package heuristics

import tabstate.Tab
import com.typesafe.scalalogging.LazyLogging

object HeuristicsEngine extends LazyLogging {
  def observe(currentTabs: List[Tab]): Thread = {
    val thread = new Thread {
      logger.info("> Starting to observe current tab state")
      while (true) {
        Thread.sleep(5000)
      }
    }

    thread.setName("HeuristicsEngine")

    thread
  }
}
