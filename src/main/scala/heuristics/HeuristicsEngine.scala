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
        if (TabSwitches.tabGraph != null) {
          logger.debug("> Cleaning tab switch graph")
          val cleanTabSwitchGraph =
            TabSwitches.cleanupGraph(TabSwitches.tabGraph)

          logger.debug("> Computing markov clusters")
          val markovClusters = Watset(cleanTabSwitchGraph)

          logger.debug("> Computing cluster titles")
          val markovClustersWithTitles = markovClusters.map(tabCluster => {
            val keywords = extractKeywordsFromTabSet(tabCluster)
            (keywords.mkString(" "), tabCluster)
          })

          // update the markov clusters stored in the webextension
          if (markovClustersWithTitles.size > 0) {
            logger.debug(
              s"> Sending updated groups to browser ${markovClustersWithTitles.toString()}"
            )

            NativeMessaging.writeNativeMessage(
              Main.out,
              HeuristicsAction("UPDATE_GROUPS", markovClustersWithTitles.asJson)
            )
          }
        }

        Thread.sleep(30000)
      }
    }

    thread.setName("HeuristicsEngine")

    thread
  }

  def extractKeywordsFromTabSet(tabSet: Set[Tab]): List[String] = {
    logger.debug(s"> Extracting keywords from set of tabs ${tabSet.toString()}")

    val allTitles =
      tabSet
        .map(tab =>
          s"${tab.normalizedTitle} ${tab.origin.split("/").drop(2).mkString(" ")}"
        )
        .mkString(" ")
    logger.debug(s"> Combined all titles in tab cluster: $allTitles")

    val keywords = KeywordExtraction(allTitles)
    logger.debug(s"> Extracted keywords from tab cluster: $keywords")

    keywords
  }
}
