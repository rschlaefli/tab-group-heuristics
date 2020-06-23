package tabstate

import akka.actor.Actor
import akka.actor.ActorLogging
import com.typesafe.scalalogging.LazyLogging
import org.slf4j.MarkerFactory
import scala.concurrent.duration._
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import scala.language.postfixOps
import java.io.BufferedOutputStream

import messaging._
import tabstate.Tab
import main.Main.StreamInit
import main.Main.StreamAck
import main.Main.StreamComplete
import main.Main.StreamFail
import CurrentTabsActor.InitializeTabs
import CurrentTabsActor.UpdateTab
import CurrentTabsActor.ActivateTab
import CurrentTabsActor.RemoveTab
import tabswitches.TabSwitchActor.TabSwitch
import heuristics.HeuristicsAction
import heuristics.HeuristicsActor.UpdateCuratedGroups

class TabStateActor extends Actor with ActorLogging with LazyLogging {

  import TabStateActor._

  val logToCsv = MarkerFactory.getMarker("CSV")

  implicit val out = new BufferedOutputStream(System.out)
  implicit val executionContext = context.dispatcher

  val currentTabs = context.actorOf(Props[CurrentTabsActor], "CurrentTabs")
  val heuristics = context.actorSelection("/user/Heuristics")
  val tabSwitches = context.actorSelection("/user/Heuristics/TabSwitches")

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

      sender() ! StreamAck
    }

    case activateEvent: TabActivateEvent => {
      val TabActivateEvent(id, windowId, previousTabId) = activateEvent

      currentTabs ! ActivateTab(previousTabId, id, windowId)

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

    case TabActivated(prevTab, tab) => {
      val message =
        s"ACTIVATE;${tab.id};${tab.hash};${tab.baseUrl};${tab.normalizedTitle}"
      logger.info(logToCsv, message)

      tabSwitches ! TabSwitch(prevTab, tab)
    }

    case message =>
      log.info(s"Received unknown TabEvent $message")

      sender() ! StreamAck
  }

}

object TabStateActor {
  case class TabActivated(prevTab: Option[Tab], tab: Tab)
  case class TabUpdated(prevState: Option[Tab], newState: Tab)
}
