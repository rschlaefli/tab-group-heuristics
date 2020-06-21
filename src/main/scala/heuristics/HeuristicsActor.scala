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

import heuristics.TabGroup
import tabswitches.TabSwitchActor.ComputeGroups
import tabswitches.TabMeta
import tabswitches.TabSwitchActor
import tabswitches.TabSwitchActor.TabSwitch

class HeuristicsActor extends Actor with ActorLogging with Timers {

  import HeuristicsActor._

  implicit val executionContext = context.dispatcher

  val tabSwitches = context.actorOf(Props[TabSwitchActor], "TabSwitches")

  var tabGroups = List[(String, Set[TabMeta])]()

  override def preStart(): Unit = {
    timers.startTimerAtFixedRate(
      "heuristics",
      ComputeHeuristics,
      2 minutes
    )
  }

  override def receive: Actor.Receive = {

    case UpdateCuratedGroups(tabGroups) => {
      log.info(s"Received tab groups $tabGroups")
    }

    case ComputeHeuristics => {
      implicit val timeout = Timeout(20 seconds)

      (tabSwitches ? ComputeGroups)
        .mapTo[TabSwitchHeuristicsResults]
        .map {
          case TabSwitchHeuristicsResults(newTabGroups) => {
            log.info(newTabGroups.toString())
            tabGroups = newTabGroups
          }
        }
    }

    case QueryTabGroups => sender() ! CurrentTabGroups(tabGroups)

    case message => log.info(s"Received message $message")

  }
}

object HeuristicsActor {
  case object ComputeHeuristics
  case object QueryTabGroups

  case class CurrentTabGroups(tabGroups: List[(String, Set[TabMeta])])
  case class UpdateCuratedGroups(tabGroups: List[TabGroup])
  case class TabSwitchHeuristicsResults(tabGroups: List[(String, Set[TabMeta])])
}
