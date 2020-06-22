package heuristics

import akka.actor.ActorLogging
import akka.actor.Actor
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import scala.language.postfixOps
import scala.concurrent.duration._
import scala.util.Success
import scala.util.Failure
import akka.actor.Timers
import io.circe._, io.circe.parser._, io.circe.generic.semiauto._,
io.circe.syntax._

import heuristics.TabGroup
import tabswitches.TabSwitchActor.ComputeGroups
import tabswitches.TabMeta
import tabswitches.TabSwitchActor
import tabswitches.TabSwitchActor.TabSwitch
import util.Utils
import messaging.NativeMessaging
import messaging.IO

class HeuristicsActor extends Actor with ActorLogging with Timers {

  import HeuristicsActor._

  implicit val executionContext = context.dispatcher

  val tabSwitches = context.actorOf(Props[TabSwitchActor], "TabSwitches")

  var tabGroupIndex = Map[Int, Int]()
  var tabGroups = List[(String, Set[TabMeta])]()
  var curatedGroups = List[(String, Set[TabMeta])]()
  var curatedGroupIndex = Map[Int, Int]()

  override def preStart(): Unit = {
    timers.startTimerAtFixedRate(
      "heuristics",
      ComputeHeuristics,
      5 minutes
    )
  }

  override def receive: Actor.Receive = {

    case UpdateCuratedGroups(tabGroups) => {
      curatedGroups = tabGroups.map(_.asTuple)
      val (curatedIndex, _) =
        Utils.processClusters(curatedGroups.map(_._2))
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
            tabGroupIndex = groupIndex
            tabGroups = newTabGroups

            if (tabGroups.size > 0) {
              log.debug(s"Updating tab clusters in the webextension")
              NativeMessaging.writeNativeMessage(
                IO.out,
                HeuristicsAction.UPDATE_GROUPS(tabGroups.map(_._2).asJson)
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
      tabGroups: List[(String, Set[TabMeta])]
  )
}
