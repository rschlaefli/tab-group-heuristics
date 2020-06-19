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
import HeuristicsActor.UpdateCuratedGroups
import HeuristicsActor.ApplyTabSwitchHeuristic
import tabswitches.TabSwitchActor.ComputeGroups
import tabswitches.TabMeta
import tabswitches.TabSwitchActor

class HeuristicsActor extends Actor with ActorLogging with Timers {

  import HeuristicsActor._

  implicit val executionContext = context.dispatcher

  val tabSwitches = context.actorOf(Props[TabSwitchActor], "TabSwitches")

  override def preStart(): Unit = {
    timers.startTimerAtFixedRate(
      "tabSwitchHeuristic",
      ApplyTabSwitchHeuristic,
      2 minutes
    )
  }

  override def receive: Actor.Receive = {

    case UpdateCuratedGroups(tabGroups) => {
      log.info(s"Received tab groups $tabGroups")
    }

    case ApplyTabSwitchHeuristic => {
      implicit val timeout = Timeout(20 seconds)
      tabSwitches ? ComputeGroups onComplete {
        case Success(TabSwitchHeuristicsResults(tabGroups)) => {
          log.info(tabGroups.toString())
        }

        case Failure(ex) => log.error(ex.getMessage())
      }
    }

    case message => {
      log.info(s"Received message $message")
    }
  }
}

object HeuristicsActor {
  case class UpdateCuratedGroups(tabGroups: List[TabGroup])
  case class TabSwitchHeuristicsResults(tabGroups: List[(String, Set[TabMeta])])

  case object ApplyTabSwitchHeuristic
}
