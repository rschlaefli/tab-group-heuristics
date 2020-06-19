package refactor

import akka.actor.Actor
import akka.actor.ActorLogging
import com.typesafe.scalalogging.LazyLogging
import org.slf4j.MarkerFactory

import tabstate.Tab

class TabSwitchActor extends Actor with ActorLogging with LazyLogging {
  import TabSwitchActor._

  val logToCsv = MarkerFactory.getMarker("CSV")

  override def receive: Actor.Receive = {
    case TabSwitch(Some(prevTab), newTab) => {
      logger.info(
        logToCsv,
        s"${prevTab.id};${prevTab.hash};${prevTab.baseUrl};${prevTab.normalizedTitle};" +
          s"${newTab.id};${newTab.hash};${newTab.baseUrl};${newTab.normalizedTitle}"
      )
    }

    case message => log.debug(s"Received message $message")
  }
}

object TabSwitchActor {
  case class TabSwitch(tab1: Option[Tab], tab2: Tab)
}
