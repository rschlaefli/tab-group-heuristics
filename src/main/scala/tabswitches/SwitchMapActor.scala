package tabswitches

import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.Actor
import akka.actor.ActorLogging
import com.typesafe.scalalogging.LazyLogging
import io.circe.parser._
import io.circe.syntax._
import org.slf4j.MarkerFactory
import persistence.Persistence
import statistics.StatisticsActor.TabSwitch
import tabstate.Tab

import SwitchGraphActor.CurrentSwitchMap

class SwitchMapActor extends Actor with ActorLogging with LazyLogging {

  import SwitchMapActor._

  val statistics = context.actorSelection("/user/Statistics")

  val logToCsv = MarkerFactory.getMarker("CSV")

  var tabSwitches = mutable.Map[String, TabSwitchMeta]()

  override def preStart(): Unit = {
    Persistence
      .restoreJson("tab_switches.json")
      .map(decode[mutable.Map[String, TabSwitchMeta]])
      .foreach {
        case Right(restoredMap) => tabSwitches = restoredMap
      }

    context.system.scheduler.scheduleAtFixedRate(30 seconds, 30 seconds) { () =>
      Persistence.persistJson("tab_switches.json", tabSwitches.asJson)
    }(context.dispatcher)
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

    case QueryTabSwitchMap => {
      sender() ! CurrentSwitchMap(tabSwitches.toMap)
    }

    case message => log.debug("Received message $message")
  }
}

object SwitchMapActor {
  case class ProcessTabSwitch(prevTab: Tab, newTab: Tab)

  case object QueryTabSwitchMap
}
