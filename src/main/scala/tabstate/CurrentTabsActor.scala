package tabstate

import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Timers
import tabstate.Tab

class CurrentTabsActor extends Actor with ActorLogging with Timers {

  import CurrentTabsActor._

  var activeTab = -1
  var activeWindow = -1
  var currentTabs = mutable.Map[Int, Tab]()
  val lastAccessed = mutable.Map[Int, Long]()

  override def receive: Actor.Receive = {
    case InitializeTabs(initialTabs) => {
      // map initial tabs such that they connect an appropriate lastAccessed timestamp
      // if there was already a timestamp in the internal mapping, reuse
      // otherwise, set the current time as the lastAccessed timestamp
      initialTabs.map(tab => {
        lastAccessed
          .get(tab.id)
          .map(tab.withAccessTs(_))
          .orElse(Some(tab.withCurrentAccessTs))
          .get
      })
    }

    case UpdateTab(tab) => {
      val prevTabState = currentTabs.get(tab.id)
      val tabWithCurrentAccessTs = tab.withCurrentAccessTs
      currentTabs(tab.id) = tabWithCurrentAccessTs
      lastAccessed(tab.id) = tabWithCurrentAccessTs.lastAccessed.get
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
          case Some(tab) => {
            val updatedTab = tab.withCurrentAccessTs
            lastAccessed(updatedTab.id) = updatedTab.lastAccessed.get
            Some(updatedTab)
          }
          case None => None
        }
        currentTabs.updateWith(previousTabId) {
          case Some(tab) => {
            val updatedTab = tab.withCurrentAccessTs
            lastAccessed(updatedTab.id) = updatedTab.lastAccessed.get
            Some(updatedTab)
          }
          case None => None
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

    case RemoveTab(tabId) => {
      // if there is a job scheduled for the tab under removal, cancel the job and remove the schedule
      timers.cancel(s"activate-$tabId")

      // remove the current tab
      currentTabs -= (tabId)
      lastAccessed -= (tabId)
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
  case class InitializeTabs(initialTabs: List[Tab])

  case class UpdateTab(tab: Tab)

  case class ActivateTab(prevTabId: Option[Int], tabId: Int, windowId: Int)

  case class RemoveTab(tabId: Int)

  case object QueryTabs
  case class CurrentTabs(tabs: List[Tab])

  case object QueryActiveTab
  case class ActiveTab(tab: Option[Tab], tabId: Int, windowId: Int)
}
