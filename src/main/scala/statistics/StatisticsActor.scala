package statistics

import org.slf4j.MarkerFactory
import scala.collection.mutable
import akka.actor.Actor
import akka.actor.ActorLogging
import com.typesafe.scalalogging.LazyLogging
import java.time.Instant
import scala.language.postfixOps
import scala.concurrent.duration._

import tabstate.Tab
import statistics._
import akka.actor.Timers

class StatisticsActor extends Actor with ActorLogging with Timers {

  import StatisticsActor._

  val logToCsv = MarkerFactory.getMarker("CSV")

  // initialize a data structure for aggregating data across windows
  val aggregationWindows: mutable.Map[Long, List[DataPoint]] = mutable.Map()

  // initialize a queue where tab switches will be pushed for analysis
  val tabSwitchQueue = mutable.Queue[TabSwitch]()

  override def preStart: Unit = {
    log.info("Starting to collect statistics")
    timers.startTimerAtFixedRate("statistics", AggregateWindows, 20 seconds)
  }

  override def receive: Actor.Receive = {
    case tabSwitch: TabSwitch => {
      log.info("Pushing tab switch to queue")
      tabSwitchQueue.enqueue(tabSwitch)
    }

    case AggregateWindows => {
      // derive the current window for aggregation
      val currentTimestamp = Instant.now.getEpochSecond()
      val minuteBlock = (currentTimestamp / 60).toLong
      log.debug(
        s"Current timestamp: ${currentTimestamp}, " +
          s"assigned block: $minuteBlock, currentMap: ${aggregationWindows.size}"
      )

    }

    case message => log.info(s"Received unknown message ${message.toString}")
  }

}

object StatisticsActor extends LazyLogging {
  case class TabSwitch(fromTab: Tab, toTab: Tab)

  case object AggregateWindows

  def computeAggregateStatistics(
      dataPoints: List[DataPoint]
  ): StatisticsOutput = {
    val output = dataPoints.foldLeft(new StatisticsData()) {
      case (acc, dataPoint) => acc.withDataPoint(dataPoint)
    }
    logger.debug(s"> Combined data into a single object ${output.toString()}")
    output.aggregated
  }
}
