package statistics

import com.typesafe.scalalogging.LazyLogging

import tabstate.TabState
import util.Utils
import org.slf4j.MarkerFactory

object StatisticsEngine extends LazyLogging {
  val logToCsv = MarkerFactory.getMarker("CSV");

  def apply(): Thread = {
    val statisticsThread = new Thread(() => {
      logger.info("> Starting to collect statistics")

      while (true) {
        Thread.sleep(5000)

        val currentlyOpenTabs = TabState.currentTabs.size

        logger.info(logToCsv, s"$currentlyOpenTabs;")
      }
    })

    statisticsThread.setName("Statistics")
    statisticsThread.setDaemon(true)

    statisticsThread
  }
}
