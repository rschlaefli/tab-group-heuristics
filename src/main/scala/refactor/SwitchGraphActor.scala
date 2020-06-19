package refactor

import akka.actor.Actor
import akka.actor.ActorLogging

class SwitchGraphActor extends Actor with ActorLogging {

  override def preStart(): Unit = {}

  override def receive: Actor.Receive = {
    case message => {}
  }
}

object SwitchGraphActor {}
