package statistics

import scala.collection.mutable
import akka.actor.Actor
import akka.actor.ActorLogging

import tabstate.Tab
import statistics._

class StatisticsActor extends Actor with ActorLogging {

  import StatisticsActor._

  // initialize a data structure for aggregating data across windows
  val aggregationWindows: mutable.Map[Long, List[DataPoint]] = mutable.Map()

  // initialize a queue where tab switches will be pushed for analysis
  val tabSwitchQueue = mutable.Queue[(Tab, Tab)]()

  override def preStart: Unit =
    log.info("Starting to collect statistics")

  override def receive: Actor.Receive = {
    case PushTabSwitch(fromTab, toTab) =>
      tabSwitchQueue.enqueue((fromTab, toTab))
    case message => log.info(s"Received unknown message ${message.toString}")
  }

}

object StatisticsActor {
  case class PushTabSwitch(fromTab: Tab, toTab: Tab)
}
