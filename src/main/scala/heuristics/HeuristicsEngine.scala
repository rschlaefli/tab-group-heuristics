package heuristics

import com.typesafe.scalalogging.LazyLogging
import scalax.collection.mutable.Graph
import scalax.collection.edge.WDiEdge
import io.circe._, io.circe.parser._, io.circe.generic.semiauto._,
io.circe.syntax._

import tabstate._
import messaging.NativeMessaging
import main.Main
import messaging.HeuristicsAction

object HeuristicsEngine extends LazyLogging {
  def apply(): Thread = {
    val thread = new Thread {
      logger.info("> Starting to observe current tab state")
      while (true) {
        val cleanTabSwitchGraph = TabSwitches.cleanupGraph(TabSwitches.tabGraph)

        val markovClusters = Watset(cleanTabSwitchGraph)

        // update the markov clusters stored in the webextension
        NativeMessaging.writeNativeMessage(
          Main.out,
          HeuristicsAction("UPDATE_GROUPS", markovClusters.asJson)
        )

        Thread.sleep(30000)
      }
    }

    thread.setName("HeuristicsEngine")

    thread
  }
}
