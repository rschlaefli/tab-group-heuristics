package tabstate

import akka.actor.ActorLogging
import akka.actor.Actor
import scala.collection.mutable

import messaging._

class TabStateActor extends Actor with ActorLogging {

  var activeTab = -1
  var activeWindow = -1
  var currentTabs = mutable.Map[Int, Tab]()

  override def preStart(): Unit =
    log.info("Starting to process tab events")

  override def receive: Actor.Receive = {
    case TabInitializationEvent(initialTabs) => {}
    case TabGroupUpdateEvent(tabGroups)      => {}
    case updateEvent: TabUpdateEvent         => {}
    case activateEvent: TabActivateEvent     => {}
    case TabRemoveEvent(id, windowId)        => {}
    case _: TabEvent                         => log.warning("Received unknown TabEvent")
  }

}

object TabStateActor {}
