package statistics

import com.typesafe.scalalogging.LazyLogging

import org.slf4j.MarkerFactory
import scala.collection.mutable

import tabstate.{Tab, TabState}
import util.Utils
import heuristics.HeuristicsEngine

object StatisticsEngine extends LazyLogging {
  val logToCsv = MarkerFactory.getMarker("CSV")

  // initialize a data structure for aggregating data across windows
  val aggregationWindows: Map[Double, Set[Int]] = Map()

  // initialize a queue where tab switches will be pushed for analysis
  val tabSwitchQueue = mutable.Queue[(Tab, Tab)]()

  def apply(): Thread = {
    val statisticsThread = new Thread(() => {
      logger.info("> Starting to collect statistics")

      while (true) {
        Thread.sleep(23000)

        // collect the current internal state for statistics computations
        val currentlyOpenTabs = TabState.currentTabs
        val currentClustering = HeuristicsEngine.clusters

        // compute the number of tabs that is currently open and not in any group
        // as well as the number of tabs that are not grouped
        val openTabHashes =
          currentlyOpenTabs.map(tab => tab._2.hashCode()).toSet
        val clusterTabHashes =
          currentClustering.flatMap(set => set.map(tab => tab.hashCode())).toSet
        val openTabsGrouped = openTabHashes.intersect(clusterTabHashes).size
        val openTabsUngrouped = openTabHashes.size - openTabsGrouped

        // TODO: push the values into a new window
        // TODO: expire windows that are older than 15min (or similar)
        // TODO: log average values across windows instead of snapshots

        logger.info(
          logToCsv,
          s"${currentlyOpenTabs.size};$openTabsGrouped;$openTabsUngrouped"
        )
      }
    })

    statisticsThread.setName("Statistics")
    statisticsThread.setDaemon(true)
    statisticsThread
  }
}
