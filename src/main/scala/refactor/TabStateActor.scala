package refactor

import akka.actor.Actor
import akka.actor.ActorLogging
import scala.collection.mutable
import com.typesafe.scalalogging.LazyLogging
import org.slf4j.MarkerFactory
import scala.concurrent.duration._
import akka.actor.Props

import Main.StreamInit
import Main.StreamAck
import Main.StreamComplete
import Main.StreamFail
import messaging._
import tabstate.Tab
import refactor.CurrentTabsActor.InitializeTabs
import refactor.CurrentTabsActor.UpdateTab
import refactor.CurrentTabsActor.ActivateTab
import refactor.CurrentTabsActor.RemoveTab

class TabStateActor extends Actor with ActorLogging with LazyLogging {
  val logToCsv = MarkerFactory.getMarker("CSV")

  val currentTabs = context.actorOf(Props[CurrentTabsActor], "CurrentTabs")

  override def preStart(): Unit = {
    log.info("Starting to process tab events")

    // query the webextension for the list of current tabs
    NativeMessaging.writeNativeMessage(IO.out, HeuristicsAction.QUERY_TABS)
  }

  override def receive: Actor.Receive = {
    case StreamInit =>
      log.info("Stream initialized")
      sender() ! StreamAck

    case StreamComplete =>
      log.info("Stream complete")
      context.stop(self)

    case StreamFail(ex) =>
      log.warning(s"Stream failed with $ex")

    case TabInitializationEvent(initialTabs) => {
      currentTabs ! InitializeTabs(initialTabs)

      sender() ! StreamAck
    }

    case updateEvent: TabUpdateEvent => {
      // build a new tab object from the received tab data
      val tab = Tab.fromEvent(updateEvent)

      currentTabs ! UpdateTab(tab)

      sender() ! StreamAck
    }

    case activateEvent: TabActivateEvent => {
      val TabActivateEvent(id, windowId, previousTabId) = activateEvent

      currentTabs ! ActivateTab(id, windowId)

      sender() ! StreamAck
    }

    case TabRemoveEvent(id, windowId) => {
      currentTabs ! RemoveTab(id)

      sender() ! StreamAck
    }

    case TabGroupUpdateEvent(tabGroups) => {
      sender() ! StreamAck
    }

    case message =>
      log.info(s"Received unknown TabEvent $message")

      sender() ! StreamAck
  }

}

object TabStateActor {}
