package heuristics

import tabstate.Tab
import com.typesafe.scalalogging.LazyLogging

object HeuristicsEngine extends LazyLogging {
  def observe(currentTabs: List[Tab]) = new Thread {
    while (true) {
      logger.debug("Heuristics engine checking tab state...")
      Thread.sleep(1000)
    }
  }
}
