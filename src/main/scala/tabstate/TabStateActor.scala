package tabstate

import java.io.BufferedOutputStream

import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import heuristics.HeuristicsAction
import heuristics.HeuristicsActor.ComputeHeuristics
import heuristics.HeuristicsActor.UpdateCuratedGroups
import main.MainActor.StartProcessing
import main.MainActor.StopProcessing
import messaging._
import org.slf4j.MarkerFactory
import tabstate.Tab
import tabswitches.TabSwitchActor.TabSwitch

import CurrentTabsActor.InitializeTabs
import CurrentTabsActor.UpdateTab
import CurrentTabsActor.ActivateTab
import CurrentTabsActor.RemoveTab

class TabStateActor extends Actor with ActorLogging with LazyLogging {

  import TabStateActor._

  val logToCsv = MarkerFactory.getMarker("CSV")

  implicit val out = new BufferedOutputStream(System.out)
  implicit val executionContext = context.dispatcher

  val currentTabs = context.actorOf(Props[CurrentTabsActor], "CurrentTabs")
  val heuristics = context.actorSelection("/user/Main/Heuristics")
  val tabSwitches = context.actorSelection("/user/Main/Heuristics/TabSwitches")

  override def preStart(): Unit = {
    log.info("Starting to process tab events")

    // query the webextension for the list of current tabs
    NativeMessaging.writeNativeMessage(HeuristicsAction.QUERY_TABS)

    // query the webextension for the list of current tab groups repeatedly
    context.system.scheduler.scheduleWithFixedDelay(10 seconds, 1 minute) {
      () => NativeMessaging.writeNativeMessage(HeuristicsAction.QUERY_GROUPS)
    }(context.system.dispatcher)
  }

  override def receive: Actor.Receive = {

    case RefreshGroupsEvent => {
      log.info("Refreshing groups")
      heuristics ! ComputeHeuristics
    }

    case PauseEvent => {
      log.info("Pausing processing")
      context.parent ! StopProcessing
    }

    case ResumeEvent => {
      log.info("Resuming processing")
      context.parent ! StartProcessing
    }

    case TabInitializationEvent(initialTabs) => {
      currentTabs ! InitializeTabs(initialTabs)

      initialTabs.foreach(tab => {
        val message =
          s"UPDATE;${tab.id};${tab.hash};${tab.baseUrl};${tab.normalizedTitle}"
        logger.info(logToCsv, message)
      })
    }

    case updateEvent: TabUpdateEvent => {
      // build a new tab object from the received tab data
      val tab = Tab.fromEvent(updateEvent)

      implicit val timeout = Timeout(1 seconds)

      val tabSwitchFuture = (currentTabs ? UpdateTab(tab))
        .mapTo[TabUpdated]
        .map {
          case TabUpdated(prevTab, newTab) => TabSwitch(prevTab, newTab)
        }

      for {
        tabSwitch <- tabSwitchFuture
        tabSwitchesRef <- tabSwitches.resolveOne
      } yield (tabSwitchesRef ! tabSwitch)

      val message =
        s"UPDATE;${tab.id};${tab.hash};${tab.baseUrl};${tab.normalizedTitle}"
      logger.info(logToCsv, message)
    }

    case activateEvent: TabActivateEvent => {
      val TabActivateEvent(id, windowId, previousTabId) = activateEvent

      currentTabs ! ActivateTab(previousTabId, id, windowId)
    }

    case TabRemoveEvent(id, windowId) => {
      logger.info(logToCsv, s"REMOVE;${id};;;")

      currentTabs ! RemoveTab(id)
    }

    case TabGroupUpdateEvent(tabGroups) => {
      logger.info(logToCsv, s"UPDATE_GROUPS;;;;")

      heuristics ! UpdateCuratedGroups(tabGroups)
    }

    case TabActivated(prevTab, tab) => {
      val message =
        s"ACTIVATE;${tab.id};${tab.hash};${tab.baseUrl};${tab.normalizedTitle}"
      logger.info(logToCsv, message)

      tabSwitches ! TabSwitch(prevTab, tab)
    }

    case message =>
      log.info(s"Received unknown TabEvent $message")
  }

}

object TabStateActor {
  case class TabActivated(prevTab: Option[Tab], tab: Tab)
  case class TabUpdated(prevState: Option[Tab], newState: Tab)
}
