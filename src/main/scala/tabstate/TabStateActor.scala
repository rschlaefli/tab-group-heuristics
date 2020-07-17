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
import communitydetection.CommunityDetectorParameters
import communitydetection.SiMapParams
import communitydetection.WatsetParams
import heuristics.HeuristicsAction
import heuristics.HeuristicsActor
import main.MainActor
import messaging._
import org.slf4j.MarkerFactory
import tabstate.CurrentTabsActor
import tabstate.Tab
import tabswitches.TabSwitchActor
import statistics.StatisticsActor

class TabStateActor extends Actor with ActorLogging with LazyLogging {

  import TabStateActor._

  val logToCsv = MarkerFactory.getMarker("CSV")

  implicit val out = new BufferedOutputStream(System.out)
  implicit val executionContext = context.dispatcher

  val currentTabs = context.actorOf(Props[CurrentTabsActor], "CurrentTabs")
  val heuristics = context.actorSelection("/user/Main/Heuristics")
  val statistics = context.actorSelection("/user/Main/Statistics")
  val tabSwitches = context.actorSelection("/user/Main/Heuristics/TabSwitches")

  override def preStart(): Unit = {
    log.info("Starting to process tab events")

    // query the webextension for the list of current tabs
    context.system.scheduler.scheduleWithFixedDelay(15 seconds, 5 minutes) {
      () => NativeMessaging.writeNativeMessage(HeuristicsAction.QUERY_TABS)
    }(context.system.dispatcher)

    // query the webextension for the list of current tab groups repeatedly
    context.system.scheduler.scheduleWithFixedDelay(30 seconds, 2 minutes) {
      () => NativeMessaging.writeNativeMessage(HeuristicsAction.QUERY_GROUPS)
    }(context.system.dispatcher)
  }

  override def receive: Actor.Receive = {

    case RefreshGroupsEvent(
        heuristicsParams,
        graphGenerationParams,
        groupingParams
        ) => {

      val groupingParamsDecoded: CommunityDetectorParameters =
        heuristicsParams.algorithm match {
          case "simap" =>
            groupingParams.as[SiMapParams].getOrElse(SiMapParams())
          case "watset" =>
            groupingParams.as[WatsetParams].getOrElse(WatsetParams())
        }

      log.info(
        s"Refreshing groups, $heuristicsParams, $graphGenerationParams, $groupingParamsDecoded"
      )

      heuristics ! HeuristicsActor.ComputeHeuristics(
        heuristicsParams,
        groupingParamsDecoded,
        graphGenerationParams
      )
    }

    case PauseEvent => {
      log.info("Pausing processing")
      context.parent ! MainActor.StopProcessing
    }

    case ResumeEvent => {
      log.info("Resuming processing")
      context.parent ! MainActor.StartProcessing
    }

    case TabInitializationEvent(initialTabs) => {
      currentTabs ! CurrentTabsActor.InitializeTabs(initialTabs)

      initialTabs.foreach(tab => {
        logger.info(
          logToCsv,
          Seq("UPDATE", tab.id, tab.hash, tab.baseUrl, tab.normalizedTitle)
            .mkString(";")
        )
      })
    }

    case updateEvent: TabUpdateEvent => {
      // build a new tab object from the received tab data
      val tab = Tab.fromEvent(updateEvent)

      implicit val timeout = Timeout(1 seconds)

      val tabSwitchFuture = (currentTabs ? CurrentTabsActor.UpdateTab(tab))
        .mapTo[TabStateActor.TabUpdated]
        .map {
          case TabStateActor.TabUpdated(prevTab, newTab) =>
            TabSwitchActor.TabSwitch(prevTab, newTab)
        }

      for {
        tabSwitch <- tabSwitchFuture
        tabSwitchesRef <- tabSwitches.resolveOne
      } yield (tabSwitchesRef ! tabSwitch)

      logger.info(
        logToCsv,
        Seq("UPDATE", tab.id, tab.hash, tab.baseUrl, tab.normalizedTitle)
          .mkString(";")
      )
    }

    case activateEvent: TabActivateEvent => {
      val TabActivateEvent(id, windowId, previousTabId) = activateEvent

      currentTabs ! CurrentTabsActor.ActivateTab(previousTabId, id, windowId)
    }

    case TabRemoveEvent(id, windowId) => {
      logger.info(logToCsv, Seq("REMOVE", id, windowId).mkString(";"))

      currentTabs ! CurrentTabsActor.RemoveTab(id)
    }

    case TabGroupUpdateEvent(tabGroups) => {
      heuristics ! HeuristicsActor.UpdateCuratedGroups(tabGroups)
    }

    case TabActivated(prevTab, tab) => {
      logger.info(
        logToCsv,
        Seq("ACTIVATE", tab.id, tab.hash, tab.baseUrl, tab.normalizedTitle)
          .mkString(";")
      )

      tabSwitches ! TabSwitchActor.TabSwitch(prevTab, tab)
    }

    case SuggestedGroupDiscardEvent(groupHash, reason, rating) =>
      heuristics ! HeuristicsActor.DiscardSuggestion(groupHash, reason, rating)

    case SuggestedGroupAcceptEvent(groupHash) =>
      heuristics ! HeuristicsActor.AcceptSuggestion(groupHash)

    case SuggestedTabAcceptEvent(groupHash, tabHash, targetGroup) =>
      heuristics ! HeuristicsActor.AcceptSuggestedTab(
        groupHash,
        tabHash,
        targetGroup
      )

    case SuggestedTabDiscardEvent(groupHash, tabHash, reason) =>
      heuristics ! HeuristicsActor.DiscardSuggestedTab(
        groupHash,
        tabHash,
        reason
      )

    case CuratedGroupOpenEvent(focusMode) =>
      statistics ! StatisticsActor.CuratedGroupOpened(focusMode)

    case CuratedGroupCloseEvent =>
      statistics ! StatisticsActor.CuratedGroupClosed

    case message =>
      log.info(s"Received unknown TabEvent $message")
  }

}

object TabStateActor {
  case class TabActivated(prevTab: Option[Tab], tab: Tab)
  case class TabUpdated(prevState: Option[Tab], newState: Tab)
}
