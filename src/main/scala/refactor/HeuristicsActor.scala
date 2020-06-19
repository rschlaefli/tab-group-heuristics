package refactor

import akka.actor.ActorLogging
import akka.actor.Actor
import akka.actor.Props

import heuristics.TabGroup
import refactor.HeuristicsActor.UpdateCuratedGroups

class HeuristicsActor extends Actor with ActorLogging {

  val tabSwitches = context.actorOf(Props[TabSwitchActor], "TabSwitches")

  override def receive: Actor.Receive = {

    case UpdateCuratedGroups(tabGroups) => {
      log.info(s"Received tab groups $tabGroups")
    }

    case message => {
      log.info(s"Received message $message")
      tabSwitches ! message
    }
  }
}

object HeuristicsActor {
  case class UpdateCuratedGroups(tabGroups: List[TabGroup])
}
