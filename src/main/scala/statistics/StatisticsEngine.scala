package statistics

import com.typesafe.scalalogging.LazyLogging

import org.slf4j.MarkerFactory
import scala.collection.mutable
import java.time.Instant
import smile.math.MathEx._

import tabstate.{Tab, TabState}
import util.Utils
import heuristics.HeuristicsEngine

object StatisticsEngine extends LazyLogging {
  val logToCsv = MarkerFactory.getMarker("CSV")

  // initialize a data structure for aggregating data across windows
  val aggregationWindows: mutable.Map[Long, List[(Int, Int, Int)]] =
    mutable.Map()

  // initialize a queue where tab switches will be pushed for analysis
  val tabSwitchQueue = mutable.Queue[(Tab, Tab)]()

  def apply(): Thread = {
    val statisticsThread = new Thread(() => {
      logger.info("> Starting to collect statistics")

      while (true) {
        Thread.sleep(3000)

        // derive the current window for aggregation
        val currentTimestamp = Instant.now.getEpochSecond()
        val fiveMinBlock = (currentTimestamp / 20).toLong
        logger.debug(
          s"> Current timestamp: ${currentTimestamp}, assigned block: $fiveMinBlock, currentMap: ${aggregationWindows.size}"
        )

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

        aggregationWindows.synchronized {
          // push the values into a window
          aggregationWindows.updateWith(fiveMinBlock) {
            case Some(list) =>
              Some(
                list.appended(
                  (currentlyOpenTabs.size, openTabsGrouped, openTabsUngrouped)
                )
              )
            case None =>
              Some(
                List(
                  (currentlyOpenTabs.size, openTabsGrouped, openTabsUngrouped)
                )
              )
          }

          logger.debug(
            s"> Updated aggregation windows to new state: $aggregationWindows"
          )

          // expire windows that are older than 5min (or similar)
          // and log the aggregate statistics for the previous window
          aggregationWindows.filterInPlace((window, data) => {
            val isWindowExpired = window <= fiveMinBlock - 1

            logger.debug(s"> Filtering window $window with data: ${data}")

            if (isWindowExpired) {
              val statistics = computeAggregateStatistics(data)

              logger.debug(s">Aggregated window $window: ${statistics}")

              logger.info(
                logToCsv,
                s"$window;${statistics._1};${statistics._2};${statistics._3}"
              )
            }

            !isWindowExpired
          })
        }

      }
    })

    statisticsThread.setName("Statistics")
    statisticsThread.setDaemon(true)
    statisticsThread
  }

  def computeAggregateStatistics(
      data: List[(Int, Int, Int)]
  ): (Double, Double, Double) = {
    val (currentTabs, groupedTabs, ungroupedTabs) = data.unzip3[Int, Int, Int]

    (
      median(currentTabs.toArray),
      median(groupedTabs.toArray),
      median(ungroupedTabs.toArray)
    )
  }
}
