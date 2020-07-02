package heuristics

import java.io.BufferedOutputStream

import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.actor.Timers
import akka.pattern.ask
import akka.util.Timeout
import groupnaming.BasicKeywords
import heuristics.TabGroup
import io.circe.syntax._
import messaging.NativeMessaging
import tabswitches.TabMeta
import tabswitches.TabSwitchActor
import tabswitches.TabSwitchActor.ComputeGroups
import tabswitches.SwitchMapActor
import tabswitches.SwitchMapActor.DiscardTabSwitch
import com.typesafe.scalalogging.LazyLogging

class HeuristicsActor extends Actor with ActorLogging with Timers {

  import HeuristicsActor._

  implicit val executionContext = context.dispatcher
  implicit val stdout = new BufferedOutputStream(System.out)

  val tabSwitches = context.actorOf(Props[TabSwitchActor], "TabSwitches")
  val switchMap = context
    .actorSelection("/user/Main/Heuristics/TabSwitches/TabSwitchMap")

  var tabGroupIndex = Map[Int, Int]()
  var tabGroups = List[TabGroup]()

  var curatedGroupIndex = Map[Int, Int]()
  var curatedGroups = List[TabGroup]()

  override def preStart(): Unit = {
    log.info("Starting to compute heuristics")

    timers.startTimerAtFixedRate(
      "heuristics",
      ComputeHeuristics,
      5 minutes
    )
  }

  override def receive: Actor.Receive = {

    case UpdateCuratedGroups(tabGroups) => {
      curatedGroups = tabGroups
      val (curatedIndex, _) =
        TabSwitchActor.buildClusterIndex(curatedGroups.map(_.tabs))
      log.info(s"Received tab groups $tabGroups with index $curatedIndex")
      curatedGroupIndex = curatedIndex
    }

    case ComputeHeuristics => {
      implicit val timeout = Timeout(20 seconds)

      log.debug("Starting heuristics computation")

      val tabGroupHashIndex = computeHashIndex(curatedGroups)

      (tabSwitches ? ComputeGroups)
        .mapTo[TabSwitchHeuristicsResults]
        .foreach {
          case TabSwitchHeuristicsResults(_, newTabGroups) => {

            if (newTabGroups.size > 0) {
              log.debug(s"Updating tab clusters in the webextension")

              val clustersWithTitles = newTabGroups.map(BasicKeywords.apply)

              tabGroups = clustersWithTitles
                .map(TabGroup.apply)
                .flatMap(group => {
                  val overlap =
                    computeSuggestionOverlap(group, tabGroupHashIndex)

                  val similarGroups = overlap
                    .filter(_._2 >= 0.2d)
                    .toList
                    .sortBy(_._2)

                  if (similarGroups.size > 0) {
                    val head = similarGroups.head
                    // if there is a superset for the suggested group, don't return it
                    if (head._2 == 1d) None
                    // otherwise, return the suggested group with the id of the existing match
                    else Some(group.withId(head._1).withoutTabs(head._3))
                  } else {
                    // if there are no similar groups, return a new suggestion
                    Some(group.withId(s"suggest-${group.id}"))
                  }
                })

              tabGroupIndex = TabSwitchActor
                .buildClusterIndex(tabGroups.map(_.tabs))
                ._1

              NativeMessaging.writeNativeMessage(
                HeuristicsAction.UPDATE_GROUPS(tabGroups.asJson)
              )
            }
          }

          case message => log.debug(s"Received $message $message")
        }
    }

    case QueryTabGroups => sender() ! CurrentTabGroups(tabGroupIndex, tabGroups)

    case AcceptSuggestion(groupHash) => {
      log.debug(s"Accepting suggested group hash $groupHash")
    }

    case DiscardSuggestion(groupHash) => {
      val targetGroup = tabGroups.find(_.id == groupHash).get

      computeHashCombinations(targetGroup)
        .foreach(switchIdentifier =>
          switchMap ! DiscardTabSwitch(switchIdentifier)
        )
    }

    case DiscardSuggestedTab(groupHash, tabHash) => {
      val targetGroup = tabGroups.find(_.id == groupHash).get

      computeHashCombinations(targetGroup)
        .filter(_.contains(tabHash))
        .foreach(switchIdentifier =>
          switchMap ! DiscardTabSwitch(switchIdentifier)
        )
    }

    case message => log.info(s"Received message $message")

  }
}

object HeuristicsActor extends LazyLogging {
  case object ComputeHeuristics
  case object QueryTabGroups

  case class CurrentTabGroups(
      groupIndex: Map[Int, Int],
      tabGroups: List[TabGroup]
  )
  case class UpdateCuratedGroups(tabGroups: List[TabGroup])
  case class TabSwitchHeuristicsResults(
      groupIndex: Map[Int, Int],
      tabGroups: List[Set[TabMeta]]
  )
  case class AcceptSuggestion(groupHash: String)
  case class DiscardSuggestion(groupHash: String)
  case class DiscardSuggestedTab(groupHash: String, tabHash: String)

  def computeHashCombinations(tabGroup: TabGroup): List[String] = {
    val tabHashes = tabGroup.tabs.map(_.hash).toList

    tabHashes
      .combinations(2)
      .map(_.sorted)
      .map { case List(hash1, hash2) => s"${hash1}_${hash2}" }
      .toList

  }

  def computeHashIndex(tabGroups: List[TabGroup]): Map[String, Set[String]] = {
    tabGroups
      .map(tabGroup => (tabGroup.id, tabGroup.tabs.map(_.hash).toSet))
      .toMap
  }

  def computeSuggestionOverlap(
      suggestedGroup: TabGroup,
      groupHashIndex: Map[String, Set[String]]
  ): List[(String, Double, Set[String])] = {
    val suggestedGroupHashes = suggestedGroup.tabs.map(_.hash)
    groupHashIndex
      .flatMap(entry => {
        val intersection = entry._2.intersect(suggestedGroupHashes)
        if (intersection.size > 0) {
          Some(
            (
              entry._1,
              // how many of the suggested groups are already present?
              intersection.size.doubleValue / suggestedGroupHashes.size
                .doubleValue(),
              intersection
            )
          )
        } else {
          None
        }
      })
      .toList
  }
}
