package refactor

import akka.actor.Actor
import akka.actor.ActorLogging
import scala.collection.mutable
import com.typesafe.scalalogging.LazyLogging
import org.slf4j.MarkerFactory
import scala.concurrent.duration._
import akka.actor.Props
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import scala.language.postfixOps

import Main.StreamInit
import Main.StreamAck
import Main.StreamComplete
import Main.StreamFail
import messaging._
import tabstate.Tab
import CurrentTabsActor.InitializeTabs
import CurrentTabsActor.UpdateTab
import CurrentTabsActor.ActivateTab
import CurrentTabsActor.RemoveTab
import CurrentTabsActor.TabActivated
import scala.util.Success
import scala.util.Failure

class TabStateActor extends Actor with ActorLogging with LazyLogging {
  val logToCsv = MarkerFactory.getMarker("CSV")

  val currentTabs = context.actorOf(Props[CurrentTabsActor], "CurrentTabs")

  implicit val executionContext = context.dispatcher
  implicit val timeout = Timeout(1 second)

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

      initialTabs.foreach(tab =>
        logger.info(
          logToCsv,
          s"UPDATE;${tab.id};${tab.hash};${tab.baseUrl};${tab.normalizedTitle}"
        )
      )

      sender() ! StreamAck
    }

    case updateEvent: TabUpdateEvent => {
      // build a new tab object from the received tab data
      val tab = Tab.fromEvent(updateEvent)

      currentTabs ! UpdateTab(tab)

      logger.info(
        logToCsv,
        s"UPDATE;${tab.id};${tab.hash};${tab.baseUrl};${tab.normalizedTitle}"
      )

      sender() ! StreamAck
    }

    case activateEvent: TabActivateEvent => {
      val TabActivateEvent(id, windowId, previousTabId) = activateEvent

      currentTabs ? ActivateTab(id, windowId) onComplete {
        case Success(TabActivated(tab)) =>
          logger.info(
            logToCsv,
            s"ACTIVATE;${tab.id};${tab.hash};${tab.baseUrl};${tab.normalizedTitle}"
          )
        case Failure(ex) => log.warning(ex.toString)
      }

      sender() ! StreamAck
    }

    case TabRemoveEvent(id, windowId) => {
      currentTabs ! RemoveTab(id)

      sender() ! StreamAck
    }

    case TabGroupUpdateEvent(tabGroups) => {
      logger.info(logToCsv, s"UPDATE_GROUPS;;;;")

      // TODO: implementation

      sender() ! StreamAck
    }

    case message =>
      log.info(s"Received unknown TabEvent $message")

      sender() ! StreamAck
  }

}

object TabStateActor {}
