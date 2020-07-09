package statistics

import java.io.BufferedOutputStream
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
import heuristics.HeuristicsAction
import heuristics.HeuristicsActor
import io.circe.parser._
import io.circe.syntax._
import messaging.NativeMessaging
import org.slf4j.MarkerFactory
import persistence.Persistence
import scalaz._
import smile.math.MathEx._
import statistics._
import tabstate.CurrentTabsActor
import tabstate.Tab

import Scalaz._

class StatisticsActor
    extends Actor
    with ActorLogging
    with Timers
    with LazyLogging {

  import StatisticsActor._

  val logToCsv = MarkerFactory.getMarker("CSV")
  val aggregateInterval = 20

  implicit val out = new BufferedOutputStream(System.out)
  implicit val executionContext = context.dispatcher

  val heuristicsActor = context.actorSelection("/user/Main/Heuristics")
  val currentTabsActor =
    context.actorSelection("/user/Main/TabState/CurrentTabs")

  // usage statistics
  var usageStatistics = UsageStatistics()

  // initialize a data structure for aggregating data across windows
  val aggregationWindows: mutable.Map[Long, List[DataPoint]] = mutable.Map()

  // initialize a queue where tab switches will be pushed for aggregation
  val tabSwitchQueue = mutable.Queue[TabSwitch]()

  // initialize a queue where suggestion interactions will be pushed for aggregation
  val suggestionInteractionsQueue = mutable.Queue[SuggestionInteraction]()

  override def preStart: Unit = {
    log.info("Starting to collect statistics")
    timers.startTimerAtFixedRate(
      "statistics",
      AggregateWindows,
      aggregateInterval seconds
    )

    val timestamp = java.time.LocalDate.now().toString()
    val usageJson = Persistence
      .restoreJson(s"usage/usage_${timestamp}.json")
      .map(decode[UsageStatistics]) foreach {
      case Right(restoredUsage) => usageStatistics = restoredUsage
      case _                    =>
    }

    log.debug(s"Restored usage data $usageJson")
  }

  override def postStop: Unit = self ! PersistState

  override def receive: Actor.Receive = {

    case PersistState => persistUsageStatistics(usageStatistics)

    case tabSwitch: TabSwitch => {
      log.info("Pushing tab switch to queue")
      tabSwitchQueue.enqueue(tabSwitch)
    }

    case suggestionInteraction: SuggestionInteraction => {
      log.info("Pushing suggestion interaction to queue")
      suggestionInteractionsQueue.enqueue(suggestionInteraction)
    }

    case RequestInteraction => {
      NativeMessaging.writeNativeMessage(HeuristicsAction.REQUEST_INTERACTION)
      usageStatistics = usageStatistics.logActionRequest()
      persistUsageStatistics(usageStatistics)
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
      val currentTabsQuery = currentTabsActor ? CurrentTabsActor.QueryTabs
      val tabGroupsQuery = heuristicsActor ? HeuristicsActor.QueryTabGroups

      val results = for {
        CurrentTabsActor.CurrentTabs(tabs) <- currentTabsQuery
        HeuristicsActor.CurrentTabGroups(groupIndex, groups) <- tabGroupsQuery
      } yield (tabs, groups, groupIndex)

      results foreach {
        case (currentTabs, tabGroups, groupIndex) => {
          log.info(s"Current tabs $currentTabs")

          val currentEpochTs = java.time.Instant.now().getEpochSecond()
          val currentTabsAge = currentTabs
            .flatMap(_.createdAt)
            .map(creationTs => (currentEpochTs - creationTs) / 60d)
            .toArray
          val currentTabsStaleness = currentTabs
            .flatMap(_.lastAccessed)
            .map(accessTs => (currentEpochTs - accessTs) / 60d)
            .toArray

          val openTabHashes = currentTabs
            .map(_.hashCode())
            .toSet
          val clusterTabHashes = tabGroups
            .flatMap(_.tabs.map(_.hashCode()))
            .toSet

          // compute the number of tabs that is currently open and not in any group
          // as well as the number of tabs that are not grouped
          val openTabsGrouped = openTabHashes.intersect(clusterTabHashes).size
          val openTabsUngrouped = openTabHashes.size - openTabsGrouped

          // prepare a new data point
          val dataPoint = new DataPoint(
            openTabHashes.size,
            openTabsUngrouped,
            openTabsGrouped,
            mean(currentTabsAge),
            mean(currentTabsStaleness)
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

          val interactionStatistics = InteractionStatistics.fromTuple(
            suggestionInteractionsQueue
              .dequeueAll(_ => true)
              .map {
                case AcceptSuggestedGroup(_) =>
                  InteractionStatistics.AcceptedGroup
                case AcceptSuggestedTab(_) =>
                  InteractionStatistics.AcceptedTab
                case DiscardSuggestedGroup(_) =>
                  InteractionStatistics.DiscardedGroup
                case DiscardSuggestedTab(_) =>
                  InteractionStatistics.DiscardedTab
              }
              .foldLeft(0, 0, 0, 0) { case (acc, stat) => acc |+| stat.value }
          )
          dataPoint.updateSuggestionInteractionStatistics(interactionStatistics)

          // log the number of interactions in usage statistics
          usageStatistics = usageStatistics
            .logInteractions(interactionStatistics.count)

          // increment the active minutes in the usage statistics
          usageStatistics = usageStatistics
            .timeIncremented(aggregateInterval)

          // if there are only very few interactions with suggestions
          if (!usageStatistics.actionRequested && usageStatistics.suggestionInteractions <= 2) {
            val strongActiveDuration = usageStatistics.activeSeconds >= 120 * 60
            val weakerActiveDuration = usageStatistics.activeSeconds >= 45 * 60
            val timeLateAfternoon = java.time.LocalTime.now().getHour() >= 15

            // if the user was active for a long time, or less time but it is late in the day
            if (strongActiveDuration || (weakerActiveDuration && timeLateAfternoon)) {
              self ! RequestInteraction
            }
          }

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
              logger.info(logToCsv, Seq(window, statistics.asCsv).mkString(";"))
            }

            !isWindowExpired
          })
        }
      }

    }

    case AggregateNow => {
      aggregationWindows.foreach {
        case (window, dataPoints) => {
          val statistics = computeAggregateStatistics(dataPoints)
          log.debug(s"Aggregated window $window: ${statistics}")
          logger.info(logToCsv, Seq(window, statistics.asCsv).mkString(";"))
        }
      }
    }

    case message => log.info(s"Received unknown message ${message.toString}")
  }

}

object StatisticsActor extends LazyLogging {
  case object PersistState
  case object RequestInteraction
  case object AggregateWindows
  case object AggregateNow

  case class TabSwitch(fromTab: Tab, toTab: Tab)

  sealed class SuggestionInteraction
  case class DiscardSuggestedGroup(groupHash: String)
      extends SuggestionInteraction
  case class DiscardSuggestedTab(groupHash: String)
      extends SuggestionInteraction
  case class AcceptSuggestedGroup(groupHash: String)
      extends SuggestionInteraction
  case class AcceptSuggestedTab(groupHash: String) extends SuggestionInteraction

  def computeAggregateStatistics(
      dataPoints: List[DataPoint]
  ): StatisticsOutput = {
    val output = dataPoints.foldLeft(new StatisticsData()) {
      case (acc, dataPoint) => acc.withDataPoint(dataPoint)
    }
    logger.debug(s"Combined data into a single object ${output.toString()}")
    output.aggregated
  }

  def persistUsageStatistics(usageStatistics: UsageStatistics) = {
    val timestamp = java.time.LocalDate.now().toString()
    Persistence.persistJson(
      s"usage/usage_${timestamp}.json",
      usageStatistics.asJson
    )
  }
}
