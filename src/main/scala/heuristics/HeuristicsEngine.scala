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
  var clusters: List[Set[Tab]] = List()

  def apply(): Thread = {
    val thread = new Thread {
      logger.info("> Starting to observe current tab state")

      while (true) {
        Thread.sleep(59000)

        if (TabSwitches.tabGraph != null) {

          // cleaning the switch graph
          logger.debug(s"> Cleaning the tab switch graph")
          val cleanTabSwitchGraph =
            TabSwitches.cleanupGraph(TabSwitches.tabGraph)

          // computing clusters
          logger.debug(s"> Computing tab clusters")
          clusters.synchronized {
            clusters = Watset(cleanTabSwitchGraph)
          }

          // generating cluster titles
          logger.debug(s"> Generating tab cluster titles")
          val clustersWithTitles = clusters.map(tabCluster => {
            val keywords = KeywordExtraction(tabCluster)
            (keywords.mkString(" "), tabCluster)
          })

          // update the markov clusters stored in the webextension
          if (clustersWithTitles.size >= 0) {
            logger.debug(s"> Updating tab clusters in the webextension")
            NativeMessaging.writeNativeMessage(
              Main.out,
              HeuristicsAction("UPDATE_GROUPS", clustersWithTitles.asJson)
            )
          }
        }
      }
    }

    thread.setName("HeuristicsEngine")
    thread
  }
}
