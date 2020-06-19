package refactor

import akka.actor.Actor
import akka.actor.ActorLogging

import tabstate.Tab

class TabSwitchActor extends Actor with ActorLogging {
  override def receive: Actor.Receive = {
    case message => log.debug(s"Received message $message")
  }
}

object TabSwitchActor {
  case class TabSwitch(tab1: Option[Tab], tab2: Tab)
}
