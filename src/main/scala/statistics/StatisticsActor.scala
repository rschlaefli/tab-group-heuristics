package statistics

import java.io.BufferedOutputStream

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
  val aggregateInterval = 30

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

  override def postStop: Unit = {
    self ! PersistState
    self ! AggregateWindows
  }

  override def receive: Actor.Receive = {

    case PersistState => {
      persistUsageStatistics(usageStatistics)
    }

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

    case CuratedGroupOpened(focusMode) => {
      eventQueue.enqueue(CuratedGroupOpenEvent(focusMode))
    }

    case CuratedGroupClosed => {
      eventQueue.enqueue(CuratedGroupCloseEvent())
    }

    case AggregateWindows => {

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
            .map(creationTs => {
              val t = (currentEpochTs - creationTs) / 60
              Age(t.intValue())
            })
            .fold(Age())(_ + _)
          val currentTabsStaleness = currentTabs
            .flatMap(_.lastAccessed)
            .map(accessTs => {
              val t = (currentEpochTs - accessTs) / 60
              Age(t.intValue())
            })
            .fold(Age())(_ + _)

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
                        StatisticsMeasurement(numSwitchesWithinGroups = 1)
                      } else {
                        StatisticsMeasurement(numSwitchesBetweenGroups = 1)
                      }
                    }
                    case (true, false) =>
                      StatisticsMeasurement(numSwitchesFromGroups = 1)
                    case (false, true) =>
                      StatisticsMeasurement(numSwitchesToGroups = 1)
                    case (false, false) =>
                      StatisticsMeasurement(numSwitchesUngrouped = 1)
                  }

                val switchTime = Age(prevTime)

                switchMeasurement + StatisticsMeasurement(
                  binSwitchTime = Seq(switchTime)
                )
              }

              case SuggestionInteractionEvent(event) =>
                event match {
                  case AcceptSuggestedGroup(_) =>
                    StatisticsMeasurement(numAcceptedGroups = 1)
                  case AcceptSuggestedTab(_) =>
                    StatisticsMeasurement(numAcceptedTabs = 1)
                  case DiscardSuggestedGroup(_, Some(reason), rating)
                      if reason == "WRONG" => {
                    StatisticsMeasurement(
                      numDiscardedGroups = 1,
                      numDiscardedWrong = 1,
                      binDiscardedRatings =
                        Seq(rating.map(Rating(_)).getOrElse(Rating()))
                    )
                  }

                  case DiscardSuggestedGroup(_, _, rating) => {
                    StatisticsMeasurement(
                      numDiscardedGroups = 1,
                      numDiscardedOther = 1,
                      binDiscardedRatings =
                        Seq(rating.map(Rating(_)).getOrElse(Rating()))
                    )
                  }
                  case DiscardSuggestedTab(_) =>
                    StatisticsMeasurement(numDiscardedTabs = 1)
                  case _ => StatisticsMeasurement()
                }

              case CuratedGroupOpenEvent(focusMode) =>
                StatisticsMeasurement(
                  numCuratedGroupsOpened = 1,
                  numFocusModeUsed = if (focusMode) 1 else 0
                )

              case CuratedGroupCloseEvent() =>
                StatisticsMeasurement(numCuratedGroupsClosed = 1)

            }
            .foldLeft(
              StatisticsMeasurement(
                numOpenTabs = openTabHashes.size,
                numOpenTabsGrouped = openTabsGrouped,
                numOpenTabsUngrouped = openTabsUngrouped,
                numCuratedGroups = curatedGroups.size,
                binTabAge = currentTabsAge,
                binTabStaleness = currentTabsStaleness
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

          log.debug(s"Aggregated statistics: ${measurement}")
          logger.info(logToCsv, measurement.asCsv)

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

  case class CuratedGroupOpened(focusMode: Boolean)
  case object CuratedGroupClosed

  def persistUsageStatistics(usageStatistics: UsageStatistics) = {
    val timestamp = java.time.LocalDate.now().toString()
    Persistence.persistJson(
      s"usage/usage_${timestamp}.json",
      usageStatistics.asJson
    )
  }
}
