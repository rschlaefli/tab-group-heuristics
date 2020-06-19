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
import scala.util.Success
import scala.util.Failure

import messaging._
import tabstate.Tab

import Main.StreamInit
import Main.StreamAck
import Main.StreamComplete
import Main.StreamFail

import CurrentTabsActor.InitializeTabs
import CurrentTabsActor.UpdateTab
import CurrentTabsActor.ActivateTab
import CurrentTabsActor.RemoveTab
import CurrentTabsActor.TabActivated
import refactor.HeuristicsActor.UpdateCuratedGroups
import refactor.TabSwitchActor.TabSwitch
import refactor.CurrentTabsActor.TabUpdated

class TabStateActor extends Actor with ActorLogging with LazyLogging {
  val logToCsv = MarkerFactory.getMarker("CSV")

  implicit val executionContext = context.dispatcher

  val currentTabs = context.actorOf(Props[CurrentTabsActor], "CurrentTabs")
  val heuristics = context.actorSelection("/user/Heuristics")
  val tabSwitches = context.actorSelection("/user/Heuristics/TabSwitches")

  override def preStart(): Unit = {
    log.info("Starting to process tab events")

    // query the webextension for the list of current tabs
    NativeMessaging.writeNativeMessage(IO.out, HeuristicsAction.QUERY_TABS)

    // query the webextension for the list of current tab groups repeatedly
    context.system.scheduler.scheduleWithFixedDelay(10 seconds, 10 seconds) {
      () =>
        NativeMessaging.writeNativeMessage(
          IO.out,
          HeuristicsAction.QUERY_GROUPS
        )
    }(context.system.dispatcher)
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

      initialTabs.foreach(tab => {
        val message =
          s"UPDATE;${tab.id};${tab.hash};${tab.baseUrl};${tab.normalizedTitle}"
        logger.info(logToCsv, message)
      })

      sender() ! StreamAck
    }

    case updateEvent: TabUpdateEvent => {
      // build a new tab object from the received tab data
      val tab = Tab.fromEvent(updateEvent)

      implicit val timeout = Timeout(1 seconds)
      currentTabs ? UpdateTab(tab) onComplete {
        case Success(TabUpdated(prevTab, newTab)) => {
          tabSwitches ! TabSwitch(prevTab, newTab)
        }
        case _ =>
      }

      val message =
        s"UPDATE;${tab.id};${tab.hash};${tab.baseUrl};${tab.normalizedTitle}"
      logger.info(logToCsv, message)

      sender() ! StreamAck
    }

    case activateEvent: TabActivateEvent => {
      val TabActivateEvent(id, windowId, previousTabId) = activateEvent

      implicit val timeout = Timeout(3 seconds)
      currentTabs ? ActivateTab(previousTabId, id, windowId) onComplete {
        case Success(TabActivated(prevTab, tab)) => {
          val message =
            s"ACTIVATE;${tab.id};${tab.hash};${tab.baseUrl};${tab.normalizedTitle}"
          logger.info(logToCsv, message)

          tabSwitches ! TabSwitch(prevTab, tab)
        }

        case Failure(ex) => log.warning(ex.toString)
      }

      sender() ! StreamAck
    }

    case TabRemoveEvent(id, windowId) => {
      logger.info(logToCsv, s"REMOVE;${id};;;")

      currentTabs ! RemoveTab(id)

      sender() ! StreamAck
    }

    case TabGroupUpdateEvent(tabGroups) => {
      logger.info(logToCsv, s"UPDATE_GROUPS;;;;")

      heuristics ! UpdateCuratedGroups(tabGroups)

      sender() ! StreamAck
    }

    case message =>
      log.info(s"Received unknown TabEvent $message")

      sender() ! StreamAck
  }

}

object TabStateActor {}
