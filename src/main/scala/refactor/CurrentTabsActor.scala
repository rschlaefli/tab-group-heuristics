package refactor

import akka.actor.Actor
import akka.actor.ActorLogging
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.collection.mutable
import akka.actor.Cancellable
import akka.actor.Timers

import TabStateActor._
import tabstate.Tab

class CurrentTabsActor extends Actor with ActorLogging with Timers {

  import CurrentTabsActor._

  var activeTab = -1
  var activeWindow = -1
  var currentTabs = mutable.Map[Int, Tab]()

  override def receive: Actor.Receive = {
    case InitializeTabs(initialTabs) => {
      currentTabs ++= initialTabs.map(tab => (tab.id, tab))
    }

    case UpdateTab(tab) => {
      val prevTabState = currentTabs.get(tab.id)
      currentTabs(tab.id) = tab
      sender() ! TabUpdated(prevTabState, tab)
    }

    case activateEvent: ActivateTab => {
      val ActivateTab(prevTabId, tabId, windowId) = activateEvent

      if (currentTabs.contains(tabId)) {
        // get the previous tab
        val previousTab = currentTabs.get(prevTabId.getOrElse(activeTab))

        // update internal representations of active tab and window
        activeTab = tabId
        activeWindow = windowId

        // let the sender know that we have completed tab activation
        context.actorSelection("/user/TabState") ! TabActivated(
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
    }

    case QueryTabs =>
      sender() ! CurrentTabs(currentTabs.values.toList)

    case QueryActiveTab =>
      sender() ! ActiveTab(currentTabs.get(activeTab), activeTab, activeWindow)
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
