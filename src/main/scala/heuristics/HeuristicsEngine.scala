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
          val cleanTabSwitchGraph =
            TabSwitches.cleanupGraph(TabSwitches.tabGraph)

          val markovClusters = Watset(cleanTabSwitchGraph)

          val markovClustersWithTitles = markovClusters.map(tabCluster => {
            val keywords = extractKeywordsFromTabSet(tabCluster)
            (keywords.mkString(" "), tabCluster)
          })

          // update the markov clusters stored in the webextension
          NativeMessaging.writeNativeMessage(
            Main.out,
            HeuristicsAction("UPDATE_GROUPS", markovClustersWithTitles.asJson)
          )
        }

        Thread.sleep(120000)
      }
    }

    thread.setName("HeuristicsEngine")

    thread
  }

  def extractKeywordsFromTabSet(tabSet: Set[Tab]): List[String] = {
    val allTitles =
      tabSet
        .map(tab =>
          s"${tab.title} ${tab.origin.split("/").drop(2).mkString(" ")}"
        )
        .mkString(" ")
    logger.info(s"> Combined all titles in tab cluster: $allTitles")

    val keywords = KeywordExtraction(allTitles)
    logger.info(s"> Extracted keywords from tab cluster: $keywords")

    keywords
  }
}
