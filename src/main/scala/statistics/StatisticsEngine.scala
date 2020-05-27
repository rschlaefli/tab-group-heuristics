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
  val aggregationWindows
      : mutable.Map[Long, List[(Int, Int, Int, Int, Int, Int, Int, Int)]] =
    mutable.Map()

  // initialize a queue where tab switches will be pushed for analysis
  val tabSwitchQueue = mutable.Queue[(Tab, Tab)]()

  def apply(): Thread = {
    val statisticsThread = new Thread(() => {
      logger.info("> Starting to collect statistics")

      while (true) {
        Thread.sleep(23000)

        // derive the current window for aggregation
        val currentTimestamp = Instant.now.getEpochSecond()
        val fiveMinBlock = (currentTimestamp / 300).toLong
        logger.debug(
          s"> Current timestamp: ${currentTimestamp}, assigned block: $fiveMinBlock, currentMap: ${aggregationWindows.size}"
        )

        // collect the current internal state for statistics computations
        val currentlyOpenTabs = TabState.currentTabs
        val currentClustering = HeuristicsEngine.clusters

        // compute the number of tabs that is currently open and not in any group
        // as well as the number of tabs that are not grouped
        val openTabHashes = currentlyOpenTabs
          .map(tab => tab._2.hashCode())
          .toSet
        val clusterTabHashes = currentClustering._2
          .flatMap(set => set.map(tab => tab.hashCode()))
          .toSet
        val openTabsGrouped = openTabHashes.intersect(clusterTabHashes).size
        val openTabsUngrouped = openTabHashes.size - openTabsGrouped

        // process the tab switch queue
        val clusterAssignments = currentClustering._1
        val clusteredTabs = clusterAssignments.keySet
        var tabSwitchStatistics: Option[(Int, Int, Int, Int, Int)] = None

        tabSwitchQueue.synchronized {
          logger.debug(
            s"> Elements in tab switch queue: ${tabSwitchQueue.toString()}"
          )

          tabSwitchStatistics = Some(
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

          logger.debug(
            s"> Tab switch queue aggregated to ${tabSwitchStatistics}"
          )
        }

        aggregationWindows.synchronized {
          val (
            switchesWithinGroups,
            switchesBetweenGroups,
            switchesFromGroups,
            switchesToGroups,
            switchesOutsideGroups
          ) =
            tabSwitchStatistics.getOrElse((0, 0, 0, 0, 0))

          // push the values into a window
          aggregationWindows.updateWith(fiveMinBlock) {
            _.map(
              _.appended(
                (
                  currentlyOpenTabs.size,
                  openTabsGrouped,
                  openTabsUngrouped,
                  switchesWithinGroups,
                  switchesBetweenGroups,
                  switchesFromGroups,
                  switchesToGroups,
                  switchesOutsideGroups
                )
              )
            ).orElse(Some(List()))
          }

          logger.debug(
            s"> Updated aggregation windows to new state: $aggregationWindows"
          )

          // expire windows that are older than 5min (or similar)
          // and log the aggregate statistics for the previous window
          aggregationWindows.filterInPlace((window, data) => {
            val isWindowExpired = window <= fiveMinBlock - 1

            logger.debug(
              s"> Filtering window $window with data: ${data}, expired: ${isWindowExpired}"
            )

            if (isWindowExpired) {
              val statistics = computeAggregateStatistics(data)

              logger.debug(s"> Aggregated window $window: ${statistics}")

              logger.info(
                logToCsv,
                s"$window;${statistics._1};${statistics._2};${statistics._3};${statistics._4};${statistics._5};${statistics._6};${statistics._7};${statistics._8}"
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
      data: List[(Int, Int, Int, Int, Int, Int, Int, Int)]
  ): (Int, Int, Int, Int, Int, Int, Int, Int) = {
    val (
      numCurrentTabs,
      openTabsGrouped,
      openTabsUngrouped,
      tabSwitchWithinGroups,
      tabSwitchBetweenGroups,
      tabSwitchFromGroup,
      tabSwitchToGroup,
      tabSwitchUngrouped
    ) = data.foldLeft(
      (
        List[Int](),
        List[Int](),
        List[Int](),
        List[Int](),
        List[Int](),
        List[Int](),
        List[Int](),
        List[Int]()
      )
    ) {
      case (
          (
            numCurrentTabs,
            openTabsGrouped,
            openTabsUngrouped,
            tabSwitchWithinGroups,
            tabSwitchBetweenGroups,
            tabSwitchFromGroup,
            tabSwitchToGroup,
            tabSwitchUngrouped
          ),
          (val1, val2, val3, val4, val5, val6, val7, val8)
          ) =>
        (
          numCurrentTabs.appended(val1),
          openTabsGrouped.appended(val2),
          openTabsUngrouped.appended(val3),
          tabSwitchWithinGroups.appended(val4),
          tabSwitchBetweenGroups.appended(val5),
          tabSwitchFromGroup.appended(val6),
          tabSwitchToGroup.appended(val7),
          tabSwitchUngrouped.appended(val8)
        )
    }

    (
      median(numCurrentTabs.toArray),
      median(openTabsGrouped.toArray),
      median(openTabsUngrouped.toArray),
      tabSwitchWithinGroups.sum,
      tabSwitchBetweenGroups.sum,
      tabSwitchFromGroup.sum,
      tabSwitchToGroup.sum,
      tabSwitchUngrouped.sum
    )
  }
}
