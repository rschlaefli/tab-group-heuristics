package refactor

import akka.actor.Actor
import akka.actor.ActorLogging
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.collection.mutable

import tabstate.Tab

class CurrentTabsActor extends Actor with ActorLogging {

  import CurrentTabsActor._

  var activeTab = -1
  var activeWindow = -1
  var currentTabs = mutable.Map[Int, Tab]()

  override def receive: Actor.Receive = {
    case InitializeTabs(initialTabs) => {
      currentTabs ++= initialTabs.map(tab => (tab.id, tab))
    }

    case UpdateTab(tab) => {
      currentTabs(tab.id) = tab
    }

    case activateEvent: ActivateTab => {
      val ActivateTab(tabId, windowId) = activateEvent

      if (currentTabs.contains(tabId)) {
        activeTab = tabId
        activeWindow = windowId
      } else {
        context.system.scheduler.scheduleOnce(400 millisecond) {
          log.debug("Tab switch to non-existent tab, pushing back to queue...")
          self ! activateEvent
        }(context.system.dispatcher)
      }
    }

    case RemoveTab(tabId) => {
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
  case class ActivateTab(tabId: Int, windowId: Int)
  case class RemoveTab(tabId: Int)

  case object QueryTabs
  case class CurrentTabs(tabs: List[Tab])

  case object QueryActiveTab
  case class ActiveTab(tab: Option[Tab], tabId: Int, windowId: Int)

}
