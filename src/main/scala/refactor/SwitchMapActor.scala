package refactor

import io.circe._, io.circe.parser._, io.circe.generic.semiauto._,
io.circe.syntax._
import scala.collection.mutable
import akka.actor.Actor
import akka.actor.ActorLogging
import com.typesafe.scalalogging.LazyLogging
import org.slf4j.MarkerFactory
import scala.language.postfixOps
import scala.concurrent.duration._

import graph.TabSwitchMeta
import tabstate.Tab
import graph.TabMeta

class SwitchMapActor extends Actor with ActorLogging with LazyLogging {

  import SwitchMapActor._

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

      // TODO: push the tab switch to statistics
    }

    case message => log.debug("Received message $message")
  }
}

object SwitchMapActor {
  case class ProcessTabSwitch(prevTab: Tab, newTab: Tab)
}
