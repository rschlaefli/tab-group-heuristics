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

  // timestamp of the last tab switch
  var prevSwitchTs: Long = -1

  // usage statistics
  var usageStatistics = UsageStatistics()

  // initialize data structures for aggregating data across windows
  val aggregationWindows: mutable.Map[Long, List[StatisticsMeasurement]] =
    mutable.Map()
  val eventQueue = mutable.Queue[StatisticsEvent]()

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

    case RequestInteraction => {
      NativeMessaging.writeNativeMessage(HeuristicsAction.REQUEST_INTERACTION)
      usageStatistics = usageStatistics.logActionRequest()
      self ! PersistState
    }

    case TabSwitch(fromTab, toTab) => {
      log.debug("Pushing tab switch to queue")

      val currentTs = java.time.Instant.now().getEpochSecond()
      val switchTime =
        if (prevSwitchTs > -1) {
          currentTs - prevSwitchTs intValue
        } else -1

      eventQueue.enqueue(
        TabSwitchEvent(
          fromTab,
          toTab,
          switchTime
        )
      )

      // set the switch timestamp
      prevSwitchTs = currentTs
    }

    case suggestionInteraction: SuggestionInteraction => {
      log.debug("Pushing suggestion interaction to queue")
      eventQueue.enqueue(SuggestionInteractionEvent(suggestionInteraction))
    }

    case curatedGroupOpen: CuratedGroupOpenEvent => {
      eventQueue.enqueue(curatedGroupOpen)
    }

    case curatedGroupClose: CuratedGroupCloseEvent => {
      eventQueue.enqueue(curatedGroupClose)
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
        HeuristicsActor.CurrentTabGroups(
          groupIndex,
          groups,
          curatedGroups,
          _
        ) <- tabGroupsQuery
      } yield (tabs, groups, groupIndex, curatedGroups)

      results foreach {
        case (currentTabs, tabGroups, groupIndex @ _, curatedGroups) => {
          log.debug(s"Current tabs $currentTabs")

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
            .map(_.hash)
            .toSet
          val clusterTabHashes = tabGroups
            .flatMap(_.tabs.map(_.hash))
            .toSet

          // compute the number of tabs that is currently open and not in any group
          // as well as the number of tabs that are not grouped
          val openTabsGrouped = openTabHashes.intersect(clusterTabHashes).size
          val openTabsUngrouped = openTabHashes.size - openTabsGrouped

          // process the tab switch queue
          log.debug(s"Elements in queue: ${eventQueue}")

          val measurement = eventQueue
            .dequeueAll(_ => true)
            .map {
              case TabSwitchEvent(prevTab, newTab, prevTime) => {
                val isPrevTabClustered =
                  clusterTabHashes.contains(prevTab.hash)
                val isNewTabClustered =
                  clusterTabHashes.contains(newTab.hash)

                val switchMeasurement =
                  (isPrevTabClustered, isNewTabClustered) match {
                    case (true, true) => {
                      if (groupIndex(prevTab.hashCode())
                            == groupIndex(newTab.hashCode())) {
                        StatisticsMeasurement(switchesWithinGroups = 1)
                      } else {
                        StatisticsMeasurement(switchesBetweenGroups = 1)
                      }
                    }
                    case (true, false) =>
                      StatisticsMeasurement(switchesFromGroups = 1)
                    case (false, true) =>
                      StatisticsMeasurement(switchesToGroups = 1)
                    case (false, false) =>
                      StatisticsMeasurement(switchesOutsideGroups = 1)
                  }

                if (prevTime < 5) {
                  switchMeasurement + StatisticsMeasurement(
                    switchTime = Seq(prevTime),
                    shortSwitches = 1
                  )
                } else {
                  switchMeasurement + StatisticsMeasurement(
                    switchTime = Seq(prevTime)
                  )
                }
              }

              case SuggestionInteractionEvent(event) =>
                event match {
                  case AcceptSuggestedGroup(_) =>
                    StatisticsMeasurement(acceptedGroups = 1)
                  case AcceptSuggestedTab(_) =>
                    StatisticsMeasurement(acceptedTabs = 1)
                  case DiscardSuggestedGroup(_, Some(reason), rating)
                      if reason == "WRONG" => {
                    StatisticsMeasurement(
                      discardedGroups = 1,
                      discardedWrong = 1,
                      discardedRating = rating
                        .map(rating => Seq(rating.doubleValue()))
                        .getOrElse(Seq())
                    )
                  }

                  case DiscardSuggestedGroup(_, _, rating) => {
                    StatisticsMeasurement(
                      discardedGroups = 1,
                      discardedOther = 1,
                      discardedRating = rating
                        .map(rating => Seq(rating.doubleValue()))
                        .getOrElse(Seq())
                    )
                  }
                  case DiscardSuggestedTab(_) =>
                    StatisticsMeasurement(discardedTabs = 1)
                  case _ => StatisticsMeasurement()
                }

              case CuratedGroupOpenEvent(focusMode) =>
                StatisticsMeasurement(
                  curatedGroupsOpened = 1,
                  focusModeUsed = if (focusMode) 1 else 0
                )

              case CuratedGroupCloseEvent() =>
                StatisticsMeasurement(curatedGroupsClosed = 1)

            }
            .foldLeft(
              StatisticsMeasurement(
                currentlyOpenTabs = openTabHashes.size,
                openTabsUngrouped = openTabsUngrouped,
                openTabsGrouped = openTabsGrouped,
                averageTabAge = mean(currentTabsAge),
                averageTabStaleDuration = mean(currentTabsStaleness),
                curatedGroups = curatedGroups.size
              )
            ) {
              case (acc, stat) => acc + stat
            }

          // log the number of interactions in usage statistics
          usageStatistics = usageStatistics
            .logInteractions(measurement.interactionCount)

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
            _.map(_.appended(measurement)).orElse(Some(List(measurement)))
          }

          log.debug(
            s"Updated aggregation windows to new state: $aggregationWindows"
          )

          // expire windows that are older than 5min (or similar)
          // and log the aggregate statistics for the previous window
          aggregationWindows.filterInPlace((window, measurements) => {
            val isWindowExpired = window < minuteBlock

            log.debug(
              s"Filtering window $window with data: ${measurements}, expired: ${isWindowExpired}"
            )

            if (isWindowExpired) {
              val statistics = computeAggregateStatistics(measurements)
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
        case (window, measurements) => {
          val statistics = computeAggregateStatistics(measurements)
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
  case class DiscardSuggestedGroup(
      groupHash: String,
      reason: Option[String],
      rating: Option[Int]
  ) extends SuggestionInteraction
  case class DiscardSuggestedTab(groupHash: String)
      extends SuggestionInteraction
  case class AcceptSuggestedGroup(groupHash: String)
      extends SuggestionInteraction
  case class AcceptSuggestedTab(groupHash: String) extends SuggestionInteraction

  def computeAggregateStatistics(
      measurements: List[StatisticsMeasurement]
  ): StatisticsOutput = {
    val output = StatisticsOutput(measurements)
    logger.debug(s"Combined data into a single object ${output.toString()}")
    output
  }

  def persistUsageStatistics(usageStatistics: UsageStatistics) = {
    val timestamp = java.time.LocalDate.now().toString()
    Persistence.persistJson(
      s"usage/usage_${timestamp}.json",
      usageStatistics.asJson
    )
  }
}
