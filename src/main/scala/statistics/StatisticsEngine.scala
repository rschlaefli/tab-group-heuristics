package statistics

import com.typesafe.scalalogging.LazyLogging

import org.slf4j.MarkerFactory
import scala.collection.mutable
import java.time.Instant
import smile.math.MathEx._

import tabstate.{Tab, TabState}
import util.Utils
import heuristics.HeuristicsEngine
import statistics._

object StatisticsEngine extends LazyLogging {
  val logToCsv = MarkerFactory.getMarker("CSV")

  // initialize a data structure for aggregating data across windows
  val aggregationWindows: mutable.Map[Long, List[DataPoint]] = mutable.Map()

  // initialize a queue where tab switches will be pushed for analysis
  val tabSwitchQueue = mutable.Queue[(Tab, Tab)]()

  def apply(): Thread = {
    val statisticsThread = new Thread(() => {
      logger.info("> Starting to collect statistics")

      while (true) {
        Thread.sleep(20370)

        // derive the current window for aggregation
        val currentTimestamp = Instant.now.getEpochSecond()
        val fiveMinBlock = (currentTimestamp / 300).toLong
        logger.debug(
          s"> Current timestamp: ${currentTimestamp}, assigned block: $fiveMinBlock, currentMap: ${aggregationWindows.size}"
        )

        // collect the current internal state for statistics computations
        val currentlyOpenTabs = TabState.currentTabs
        val currentClustering = HeuristicsEngine.clusters
        val openTabHashes = currentlyOpenTabs
          .map(tab => tab._2.hashCode())
          .toSet
        val clusterTabHashes = currentClustering._2
          .flatMap(set => set.map(tab => tab.hashCode()))
          .toSet

        // compute the number of tabs that is currently open and not in any group
        // as well as the number of tabs that are not grouped
        val openTabsGrouped = openTabHashes.intersect(clusterTabHashes).size
        val openTabsUngrouped = openTabHashes.size - openTabsGrouped

        // prepare a new data point
        val dataPoint = new DataPoint(
          currentlyOpenTabs.size,
          openTabsUngrouped,
          openTabsGrouped
        )

        // process the tab switch queue
        val clusterAssignments = currentClustering._1
        val clusteredTabs = clusterAssignments.keySet

        tabSwitchQueue.synchronized {
          logger.debug(
            s"> Elements in tab switch queue: ${tabSwitchQueue.toString()}"
          )

          val switchStatistics =
            SwitchStatistics.fromTuple(
              tabSwitchQueue
                .dequeueAll(_ => true)
                .map(tabSwitch => {
                  val (prevTab, newTab) = tabSwitch

                  val isPrevTabClustered =
                    clusteredTabs.contains(prevTab.hashCode())
                  val isNewTabClustered =
                    clusteredTabs.contains(newTab.hashCode())

                  if (isPrevTabClustered && isNewTabClustered) {
                    // switch within a group
                    if (clusterAssignments(prevTab.hashCode())
                          == clusterAssignments(newTab.hashCode())) {
                      (1, 0, 0, 0, 0)
                    } else {
                      (0, 1, 0, 0, 0)
                    }
                  } else if (isPrevTabClustered) {
                    (0, 0, 1, 0, 0)
                  } else if (isNewTabClustered) {
                    (0, 0, 0, 1, 0)
                  } else {
                    (0, 0, 0, 0, 1)
                  }
                })
                .foldLeft((0, 0, 0, 0, 0)) {
                  case (
                      (acc1, acc2, acc3, acc4, acc5),
                      (val1, val2, val3, val4, val5)
                      ) =>
                    (
                      acc1 + val1,
                      acc2 + val2,
                      acc3 + val3,
                      acc4 + val4,
                      acc5 + val5
                    )
                }
            )

          dataPoint.updateSwitchStatistics(switchStatistics)

          logger.debug(
            s"> Tab switch queue aggregated to ${switchStatistics}"
          )
        }

        aggregationWindows.synchronized {

          // push the values into a window
          aggregationWindows.updateWith(fiveMinBlock) {
            _.map(_.appended(dataPoint)).orElse(Some(List(dataPoint)))
          }

          logger.debug(
            s"> Updated aggregation windows to new state: $aggregationWindows"
          )

          // expire windows that are older than 5min (or similar)
          // and log the aggregate statistics for the previous window
          aggregationWindows.filterInPlace((window, dataPoints) => {
            val isWindowExpired = window <= fiveMinBlock - 1

            logger.debug(
              s"> Filtering window $window with data: ${dataPoints}, expired: ${isWindowExpired}"
            )

            if (isWindowExpired) {
              val statistics = computeAggregateStatistics(dataPoints)
              logger.debug(s"> Aggregated window $window: ${statistics}")
              logger.info(logToCsv, s"$window;${statistics.asCsv}")
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
      dataPoints: List[DataPoint]
  ): StatisticsOutput = {
    val output = dataPoints.foldLeft(new StatisticsData()) {
      case (acc, dataPoint) => acc.withDataPoint(dataPoint)
    }
    logger.debug(s"> Combined data into a single object ${output.toString()}")
    output.aggregated
  }
}
