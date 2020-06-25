package tabswitches

import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Timers
import com.typesafe.scalalogging.LazyLogging
import io.circe.parser._
import io.circe.syntax._
import org.slf4j.MarkerFactory
import persistence.Persistence
import statistics.StatisticsActor.TabSwitch
import tabstate.Tab

import SwitchGraphActor.CurrentSwitchMap

class SwitchMapActor
    extends Actor
    with ActorLogging
    with LazyLogging
    with Timers {

  import SwitchMapActor._

  val statistics = context.actorSelection("/user/Main/Statistics")

  val logToCsv = MarkerFactory.getMarker("CSV")

  var tabSwitches = mutable.Map[String, TabSwitchMeta]()

  override def preStart(): Unit = {
    restoreTabSwitchMap foreach {
      case Right(restoredMap) => tabSwitches = restoredMap
      case _                  =>
    }

    timers.startTimerWithFixedDelay("persist", PersistSwitchMap, 60 seconds)
  }

  override def postStop(): Unit = {
    log.info("Persisting tab switch map due to processing stop")
    self ! PersistSwitchMap
  }

  override def receive: Actor.Receive = {
    case ProcessTabSwitch(prevTab, newTab) => {
      val message =
        s"${prevTab.id};${prevTab.hash};${prevTab.baseUrl};${prevTab.normalizedTitle};" +
          s"${newTab.id};${newTab.hash};${newTab.baseUrl};${newTab.normalizedTitle}"
      logger.info(logToCsv, message)

      val List(meta1, meta2) =
        List(prevTab, newTab)
          .map(TabMeta.apply)
          .sortBy(tabMeta => tabMeta.hash)

      // update the tab switch meta information
      val switchIdentifier = s"${meta1.hash}_${meta2.hash}"
      tabSwitches.updateWith(switchIdentifier)(TabSwitchMeta(_, meta1, meta2))

      // push the tab switch to statistics
      statistics ! TabSwitch(prevTab, newTab)
    }

    case QueryTabSwitchMap =>
      sender() ! CurrentSwitchMap(tabSwitches.toMap)

    case PersistSwitchMap =>
      Persistence.persistJson("tab_switches.json", tabSwitches.asJson)

    case message => log.debug(s"Received message $message")
  }
}

object SwitchMapActor {

  case object QueryTabSwitchMap
  case object PersistSwitchMap

  case class ProcessTabSwitch(prevTab: Tab, newTab: Tab)

  def restoreTabSwitchMap = {
    Persistence
      .restoreJson("tab_switches.json")
      .map(decode[mutable.Map[String, TabSwitchMeta]])
  }
}
