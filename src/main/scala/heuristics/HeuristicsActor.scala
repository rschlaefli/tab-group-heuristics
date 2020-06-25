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
import util.Utils

class HeuristicsActor extends Actor with ActorLogging with Timers {

  import HeuristicsActor._

  implicit val executionContext = context.dispatcher
  implicit val stdout = new BufferedOutputStream(System.out)

  val tabSwitches = context.actorOf(Props[TabSwitchActor], "TabSwitches")

  var tabGroupIndex = Map[Int, Int]()
  var tabGroups = List[(String, Set[TabMeta])]()
  var curatedGroups = List[(String, Set[TabMeta])]()
  var curatedGroupIndex = Map[Int, Int]()

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
      curatedGroups = tabGroups.map(_.asTuple)
      val (curatedIndex, _) = Utils.buildClusterIndex(curatedGroups.map(_._2))
      log.info(s"Received tab groups $tabGroups with index $curatedIndex")
      curatedGroupIndex = curatedIndex
    }

    case ComputeHeuristics => {
      implicit val timeout = Timeout(20 seconds)

      log.debug("Starting heuristics computation")

      (tabSwitches ? ComputeGroups)
        .mapTo[TabSwitchHeuristicsResults]
        .foreach {
          case TabSwitchHeuristicsResults(groupIndex, newTabGroups) => {

            if (newTabGroups.size > 0) {
              log.debug(s"Updating tab clusters in the webextension")

              val clustersWithTitles = newTabGroups.map(BasicKeywords.apply)

              tabGroupIndex = groupIndex
              tabGroups = clustersWithTitles

              val tabGroupEntities = tabGroups.map(TabGroup.apply)

              NativeMessaging.writeNativeMessage(
                HeuristicsAction.UPDATE_GROUPS(tabGroupEntities.asJson)
              )
            }
          }

          case message => log.debug(s"Received $message $message")
        }
    }

    case QueryTabGroups => sender() ! CurrentTabGroups(tabGroupIndex, tabGroups)

    case message => log.info(s"Received message $message")

  }
}

object HeuristicsActor {
  case object ComputeHeuristics
  case object QueryTabGroups

  case class CurrentTabGroups(
      groupIndex: Map[Int, Int],
      tabGroups: List[(String, Set[TabMeta])]
  )
  case class UpdateCuratedGroups(tabGroups: List[TabGroup])
  case class TabSwitchHeuristicsResults(
      groupIndex: Map[Int, Int],
      tabGroups: List[Set[TabMeta]]
  )
}
