package tabswitches

import scala.collection.mutable

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Timers
import com.typesafe.scalalogging.LazyLogging
import io.circe.parser._
import io.circe.syntax._
import org.slf4j.MarkerFactory
import persistence.Persistence
import statistics.StatisticsActor
import tabstate.Tab
import tabswitches.SwitchGraphActor

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
      case Right(restoredMap) =>
        tabSwitches = restoredMap
        tabSwitches =
          tabSwitches.mapValuesInPlace((_, meta) => TabSwitchMeta.clone(meta))
      case _ =>
    }
  }

  override def postStop(): Unit = {
    log.info("Persisting tab switch map due to processing stop")
    self ! PersistState
  }

  override def receive: Actor.Receive = {

    case PersistState =>
      Persistence.persistJson("tab_switches.json", tabSwitches.asJson)

    case ProcessTabSwitch(prevTab, newTab) => {
      logger.info(
        logToCsv,
        Seq(
          prevTab.id,
          prevTab.hash,
          prevTab.baseUrl,
          prevTab.normalizedTitle,
          newTab.id,
          newTab.hash,
          newTab.baseUrl,
          newTab.normalizedTitle
        ).mkString(";")
      )

      // update the tab switch meta information
      val (meta1, meta2, switchIdentifier) =
        computeSwitchIdentifier(prevTab, newTab)
      tabSwitches.updateWith(switchIdentifier)(TabSwitchMeta(_, meta1, meta2))

      // push the tab switch to statistics
      statistics ! StatisticsActor.TabSwitch(prevTab, newTab)
    }

    case QueryTabSwitchMap =>
      sender() ! SwitchGraphActor.CurrentSwitchMap(tabSwitches.toMap)

    case DiscardTabSwitch(switchIdentifier) =>
      tabSwitches.updateWith(switchIdentifier) {
        case Some(value) => Some(value.discarded)
        case None        => None
      }

    case message => log.debug(s"Received message $message")
  }
}

object SwitchMapActor {

  case object PersistState

  case object QueryTabSwitchMap

  case class ProcessTabSwitch(prevTab: Tab, newTab: Tab)
  case class DiscardTabSwitch(switchIdenfitier: String)

  def restoreTabSwitchMap = {
    Persistence
      .restoreJson("tab_switches.json")
      .map(decode[mutable.Map[String, TabSwitchMeta]])
  }

  def computeSwitchIdentifier(
      prevTab: Tab,
      newTab: Tab
  ): (TabMeta, TabMeta, String) = {
    computeSwitchIdentifier(TabMeta(prevTab), TabMeta(newTab))
  }

  def computeSwitchIdentifier(
      prevTab: TabMeta,
      newTab: TabMeta
  ): (TabMeta, TabMeta, String) = {
    val List(meta1, meta2) =
      List(prevTab, newTab).sortBy(tabMeta => tabMeta.hash)
    (meta1, meta2, s"${meta1.hash}_${meta2.hash}")
  }

  def findRelevantSwitches(
      switchMap: Map[String, TabSwitchMeta],
      tab: TabMeta
  ): Seq[String] = {
    switchMap
      .filter(_._1.contains(tab.hash))
      .keys
      .toSeq
  }
}
