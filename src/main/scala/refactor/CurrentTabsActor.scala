package refactor

import akka.actor.Actor
import akka.actor.ActorLogging
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.collection.mutable
import akka.actor.Cancellable

import tabstate.Tab

class CurrentTabsActor extends Actor with ActorLogging {

  import CurrentTabsActor._

  var activeTab = -1
  var activeWindow = -1
  var currentTabs = mutable.Map[Int, Tab]()
  var scheduledEvent: Option[(Int, Cancellable)] = None

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
        // if we are activating an event, and there is a leftover scheduled event for the current tab
        // remove the old scheduled event to make space for other ones
        scheduledEvent
          .filter(_._1 == tabId)
          .foreach(_ => scheduledEvent = None)

        activeTab = tabId
        activeWindow = windowId

        sender() ! TabActivated(currentTabs(tabId))
      } else {
        val cancellable =
          context.system.scheduler.scheduleOnce(400 millisecond) {
            log.debug(
              "Tab switch to non-existent tab, pushing back to queue..."
            )
            self forward activateEvent
          }(context.system.dispatcher)

        // store a reference to the schedule for potential cancellation
        scheduledEvent = Some(tabId, cancellable)
      }
    }

    case RemoveTab(tabId) => {
      // if there is a job scheduled for the tab under removal, cancel the job and remove the schedule
      scheduledEvent
        .filter(_._1 == tabId)
        .foreach(job => {
          job._2.cancel()
          scheduledEvent = None
        })

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

  case class ActivateTab(tabId: Int, windowId: Int)
  case class TabActivated(tab: Tab)

  case class RemoveTab(tabId: Int)

  case object QueryTabs
  case class CurrentTabs(tabs: List[Tab])

  case object QueryActiveTab
  case class ActiveTab(tab: Option[Tab], tabId: Int, windowId: Int)

}
