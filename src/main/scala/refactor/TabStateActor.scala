package refactor

import akka.actor.Actor
import akka.actor.ActorLogging
import scala.collection.mutable
import com.typesafe.scalalogging.LazyLogging
import org.slf4j.MarkerFactory
import scala.concurrent.duration._

import Main.StreamInit
import Main.StreamAck
import Main.StreamComplete
import Main.StreamFail
import messaging._
import tabstate.Tab

class TabStateActor extends Actor with ActorLogging with LazyLogging {
  val logToCsv = MarkerFactory.getMarker("CSV")

  var activeTab = -1
  var activeWindow = -1
  var currentTabs = mutable.Map[Int, Tab]()

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

    case StreamAck => ()

    case TabInitializationEvent(initialTabs) => {
      currentTabs ++= initialTabs.map(tab => {
        logger.info(
          logToCsv,
          s"UPDATE;${tab.id};${tab.hash};${tab.baseUrl};${tab.normalizedTitle}"
        )
        (tab.id, tab)
      })

      log.info(
        s"Initialized current tabs to $currentTabs"
      )

      sender() ! StreamAck
    }

    case updateEvent: TabUpdateEvent => {
      // build a new tab object from the received tab data
      val tab = Tab.fromEvent(updateEvent)

      val prevTabState = currentTabs.get(tab.id)

      log.debug(s"Updating tab from $prevTabState to $tab")

      logger.info(
        logToCsv,
        s"UPDATE;${tab.id};${tab.hash};${tab.baseUrl};${tab.normalizedTitle}"
      )

      currentTabs.update(tab.id, tab)

      // TODO: messaging
      // if (prevTabState.isDefined) {
      //   TabSwitchMap.processTabSwitch(prevTabState, tab)
      // }

      sender() ! StreamAck
    }

    case activateEvent: TabActivateEvent => {
      val TabActivateEvent(id, windowId, previousTabId) = activateEvent

      val previousTab = currentTabs.get(previousTabId.getOrElse(activeTab))
      val currentTab = currentTabs.get(id)

      log.debug(
        s"Processing switch from $previousTab to $currentTab in window $windowId"
      )

      if (!currentTab.isDefined) {
        import scala.language.postfixOps
        context.system.scheduler.scheduleOnce(500 millisecond) {
          log.debug("Tab switch to non-existent tab, pushing back to queue...")
          self ! activateEvent
        }(context.system.dispatcher)
      } else {
        activeTab = id
        activeWindow = windowId

        logger.info(
          logToCsv,
          s"ACTIVATE;${currentTab.get.id};${currentTab.get.hash};${currentTab.get.baseUrl};${currentTab.get.normalizedTitle}"
        )

        // TODO: messaging
        // TabSwitchMap.processTabSwitch(previousTab, currentTab.get)
      }

      sender() ! StreamAck
    }

    case TabRemoveEvent(id, windowId) => {
      import scala.language.postfixOps
      context.system.scheduler.scheduleOnce(1 second) {
        logger.info(logToCsv, s"REMOVE;${id};;;")
        currentTabs -= (id)
      }(context.system.dispatcher)

      sender() ! StreamAck
    }

    case TabGroupUpdateEvent(tabGroups) => {
      log.debug(s"Updating tab groups to $tabGroups")

      logger.info(logToCsv, s"UPDATE_GROUPS;;;;")

      // TODO: messaging
      // HeuristicsEngine.updateManualClusters(tabGroups)

      sender() ! StreamAck
    }

    case message =>
      log.info(s"Received unknown TabEvent $message")
      sender() ! StreamAck
  }

}

object TabStateActor {}
