package tabstate

import java.io.BufferedOutputStream

import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Timers
import heuristics.HeuristicsAction
import io.circe.syntax._
import messaging.NativeMessaging
import tabstate.Tab

class CurrentTabsActor extends Actor with ActorLogging with Timers {

  import CurrentTabsActor._

  implicit val out = new BufferedOutputStream(System.out)

  var activeTab = -1
  var activeWindow = -1
  var currentTabs = mutable.Map[Int, Tab]()

  override def preStart(): Unit = {
    timers.startTimerWithFixedDelay("cleanup", FilterStaleTabs, 5 minutes)
  }

  override def receive: Actor.Receive = {

    case FilterStaleTabs => {
      val currentTs = java.time.Instant.now().getEpochSecond()

      // compute a list of stale tabs (i.e., tabs that have not been accessed in a long time)
      val staleTabs = currentTabs
        .filter {
          case (_, tab) =>
            tab.id != activeTab && tab.lastAccessed.fold(false)(time =>
              time > 0 && (currentTs - time) >= 60 * 60
            )
        }

      log.debug(s"stale tabs ${currentTabs.toString()} ${staleTabs.toString()}")

      // update stale tabs in the webextension
      NativeMessaging.writeNativeMessage(
        HeuristicsAction.STALE_TABS(
          staleTabs.values
            .map(_.hash)
            .toSet
            .asJson
        )
      )
    }

    case InitializeTabs(initialTabs) => {
      // map initial tabs such that they connect an appropriate lastAccessed timestamp
      // if there was already a timestamp in the internal mapping, reuse
      // otherwise, set the current time as the lastAccessed timestamp
      currentTabs = mutable.Map.from(
        initialTabs
          .map(tab => {
            currentTabs
              .get(tab.id)
              .flatMap(_.lastAccessed.map(tab.withAccessTs(_)))
              .getOrElse(tab.withCurrentAccessTs)
          })
          .map(tab => (tab.id, tab))
      )
    }

    case UpdateTab(tab) => {
      val prevTabState = currentTabs.get(tab.id)
      currentTabs(tab.id) = tab.withCurrentAccessTs
      sender() ! TabStateActor.TabUpdated(prevTabState, tab)
    }

    case activateEvent: ActivateTab => {
      val ActivateTab(prevTabId, tabId, windowId) = activateEvent

      if (currentTabs.contains(tabId)) {
        // get the previous tab
        val previousTabId = prevTabId.getOrElse(activeTab)
        val previousTab = currentTabs.get(previousTabId)

        // update internal representations of active tab and window
        activeTab = tabId
        activeWindow = windowId

        // update the lastAccessed property of the newly activated and previous tabs
        currentTabs.updateWith(tabId) {
          case Some(tab) => Some(tab.withCurrentAccessTs)
          case None      => None
        }
        currentTabs.updateWith(previousTabId) {
          case Some(tab) => Some(tab.withCurrentAccessTs)
          case None      => None
        }

        // let the sender know that we have completed tab activation
        context.actorSelection("/user/Main/TabState") ! TabStateActor
          .TabActivated(
            previousTab,
            currentTabs(tabId)
          )
      } else {
        timers.startSingleTimer(
          s"activate-$tabId",
          activateEvent,
          400 milliseconds
        )
      }
    }

    case removeEvent: RemoveTab => {
      val RemoveTab(tabId) = removeEvent

      if (currentTabs.contains(tabId)) {
        // if there is a job scheduled for the tab under removal, cancel the job and remove the schedule
        timers.cancel(s"activate-$tabId")

        // remove the current tab
        currentTabs -= (tabId)
      } else {
        timers.startSingleTimer(s"remove-$tabId", removeEvent, 1 second)
      }
    }

    case QueryTabs =>
      sender() ! CurrentTabsActor.CurrentTabs(currentTabs.values.toList)

    case QueryActiveTab =>
      sender() ! CurrentTabsActor.ActiveTab(
        currentTabs.get(activeTab),
        activeTab,
        activeWindow
      )
  }

}

object CurrentTabsActor {

  case object FilterStaleTabs

  case class InitializeTabs(initialTabs: List[Tab])

  case class UpdateTab(tab: Tab)

  case class ActivateTab(prevTabId: Option[Int], tabId: Int, windowId: Int)

  case class RemoveTab(tabId: Int)

  case object QueryTabs
  case class CurrentTabs(tabs: List[Tab])

  case object QueryActiveTab
  case class ActiveTab(tab: Option[Tab], tabId: Int, windowId: Int)

}
