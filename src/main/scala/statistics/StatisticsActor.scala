package statistics

import java.time.Instant

import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Timers
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import heuristics.HeuristicsActor.CurrentTabGroups
import heuristics.HeuristicsActor.QueryTabGroups
import org.slf4j.MarkerFactory
import scalaz._
import statistics._
import tabstate.CurrentTabsActor.CurrentTabs
import tabstate.CurrentTabsActor.QueryTabs
import tabstate.Tab

import Scalaz._

class StatisticsActor
    extends Actor
    with ActorLogging
    with Timers
    with LazyLogging {

  import StatisticsActor._

  implicit val executionContext = context.dispatcher

  val heuristicsActor = context.actorSelection("/user/Main/Heuristics")
  val currentTabsActor =
    context.actorSelection("/user/Main/TabState/CurrentTabs")

  val logToCsv = MarkerFactory.getMarker("CSV")

  // initialize a data structure for aggregating data across windows
  val aggregationWindows: mutable.Map[Long, List[DataPoint]] = mutable.Map()

  // initialize a queue where tab switches will be pushed for analysis
  val tabSwitchQueue = mutable.Queue[TabSwitch]()

  override def preStart: Unit = {
    log.info("Starting to collect statistics")
    timers.startTimerAtFixedRate("statistics", AggregateWindows, 20 seconds)
  }

  override def receive: Actor.Receive = {
    case tabSwitch: TabSwitch => {
      log.info("Pushing tab switch to queue")
      tabSwitchQueue.enqueue(tabSwitch)
    }

    case AggregateWindows => {
      // derive the current window for aggregation
      val currentTimestamp = Instant.now.getEpochSecond()
      val minuteBlock = (currentTimestamp / 60).toLong
      log.debug(
        s"Current timestamp: ${currentTimestamp}, " +
          s"assigned block: $minuteBlock, currentMap: ${aggregationWindows.size}"
      )

      // query the current tabs and tab groups from the other actors
      implicit val timeout = Timeout(1 second)
      val currentTabsQuery = currentTabsActor ? QueryTabs
      val tabGroupsQuery = heuristicsActor ? QueryTabGroups

      val results = for {
        CurrentTabs(tabs) <- currentTabsQuery
        CurrentTabGroups(groupIndex, groups) <- tabGroupsQuery
      } yield (tabs, groups, groupIndex)

      results foreach {
        case (currentTabs, tabGroups, groupIndex) => {
          log.debug(s"Queried current tabs and tab groups from other actors")

          val openTabHashes = currentTabs.map(_.hashCode()).toSet
          val clusterTabHashes = tabGroups.flatMap(_._2.map(_.hashCode())).toSet

          // compute the number of tabs that is currently open and not in any group
          // as well as the number of tabs that are not grouped
          val openTabsGrouped = openTabHashes.intersect(clusterTabHashes).size
          val openTabsUngrouped = openTabHashes.size - openTabsGrouped

          // prepare a new data point
          val dataPoint = new DataPoint(
            openTabHashes.size,
            openTabsUngrouped,
            openTabsGrouped
          )

          // process the tab switch queue
          log.debug(
            s"Elements in tab switch queue: ${tabSwitchQueue.size}"
          )

          val switchStatistics =
            SwitchStatistics.fromTuple(
              tabSwitchQueue
                .dequeueAll(_ => true)
                .map {
                  case TabSwitch(prevTab, newTab) => {
                    val isPrevTabClustered =
                      clusterTabHashes.contains(prevTab.hashCode())
                    val isNewTabClustered =
                      clusterTabHashes.contains(newTab.hashCode())

                    (isPrevTabClustered, isNewTabClustered) match {
                      case (true, true) => {
                        if (groupIndex(prevTab.hashCode())
                              == groupIndex(newTab.hashCode())) {
                          SwitchStatistics.SwitchWithinGroup
                        } else {
                          SwitchStatistics.SwitchBetweenGroups
                        }
                      }
                      case (true, false)  => SwitchStatistics.SwitchFromGroup
                      case (false, true)  => SwitchStatistics.SwitchToGroup
                      case (false, false) => SwitchStatistics.SwitchOutside
                    }
                  }
                }
                .foldLeft(0, 0, 0, 0, 0) {
                  case (acc, switch) => acc |+| switch.value
                }
            )

          log.debug(
            s"Tab switch queue aggregated to ${switchStatistics}"
          )

          dataPoint.updateSwitchStatistics(switchStatistics)

          // push the values into a window
          aggregationWindows.updateWith(minuteBlock) {
            _.map(_.appended(dataPoint)).orElse(Some(List(dataPoint)))
          }

          log.debug(
            s"Updated aggregation windows to new state: $aggregationWindows"
          )

          // expire windows that are older than 5min (or similar)
          // and log the aggregate statistics for the previous window
          aggregationWindows.filterInPlace((window, dataPoints) => {
            val isWindowExpired = window < minuteBlock

            log.debug(
              s"Filtering window $window with data: ${dataPoints}, expired: ${isWindowExpired}"
            )

            if (isWindowExpired) {
              val statistics = computeAggregateStatistics(dataPoints)
              log.debug(s"Aggregated window $window: ${statistics}")
              logger.info(logToCsv, s"$window;${statistics.asCsv}")
            }

            !isWindowExpired
          })
        }
      }

    }

    case message => log.info(s"Received unknown message ${message.toString}")
  }

}

object StatisticsActor extends LazyLogging {
  case object AggregateWindows

  case class TabSwitch(fromTab: Tab, toTab: Tab)

  def computeAggregateStatistics(
      dataPoints: List[DataPoint]
  ): StatisticsOutput = {
    val output = dataPoints.foldLeft(new StatisticsData()) {
      case (acc, dataPoint) => acc.withDataPoint(dataPoint)
    }
    logger.debug(s"Combined data into a single object ${output.toString()}")
    output.aggregated
  }
}
