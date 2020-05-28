package heuristics

import scala.collection.JavaConverters._
import com.typesafe.scalalogging.LazyLogging
import scala.collection.mutable
import scalax.collection.mutable.Graph
import scalax.collection.edge.WDiEdge
import io.circe._, io.circe.parser._, io.circe.generic.semiauto._,
io.circe.syntax._
import java.{util => ju}

import tabstate._
import messaging._

object HeuristicsEngine extends LazyLogging {
  var automatedClusters: (mutable.Map[Int, Int], List[Set[Tab]]) =
    (mutable.Map(), List())
  var automatedTitles: List[String] = List()

  var manualClusters: (mutable.Map[Int, Int], List[Set[Tab]]) =
    (mutable.Map(), List())

  def apply(): Thread = {
    val thread = new Thread {
      logger.info("> Starting to observe current tab state")

      while (true) {
        Thread.sleep(590000)

        if (TabSwitches.tabGraph != null) {

          // TODO: incorporate the manually created clusters in some way

          // cleaning the switch graph
          logger.debug(s"> Cleaning the tab switch graph")
          val cleanTabSwitchGraph =
            TabSwitches.cleanupGraph(TabSwitches.tabGraph)

          // computing clusters
          logger.debug(s"> Computing tab clusters")
          automatedClusters.synchronized {
            val computedClusters = Watset(cleanTabSwitchGraph)
            automatedClusters = processClusters(computedClusters)
          }

          // generating cluster titles
          logger.debug(s"> Generating tab cluster titles")
          automatedTitles.synchronized {
            val clustersWithTitles = automatedClusters._2.map(tabCluster => {
              val keywords = KeywordExtraction(tabCluster)
              (keywords.mkString(" "), tabCluster)
            })
            automatedTitles = clustersWithTitles.map(_._1)

            // update the markov clusters stored in the webextension
            if (clustersWithTitles.size >= 0) {
              logger.debug(s"> Updating tab clusters in the webextension")
              NativeMessaging.writeNativeMessage(
                IO.out,
                HeuristicsAction.UPDATE_GROUPS(clustersWithTitles.asJson)
              )
            }
          }
        }
      }
    }

    thread.setName("HeuristicsEngine")
    thread
  }

  def processClusters(
      clusters: List[Set[Tab]]
  ): (mutable.Map[Int, Int], List[Set[Tab]]) = {
    // preapre an index for which tab is stored in which cluster
    val clusterIndex = mutable.Map[Int, Int]()

    // prepare a return container for the clusters
    val clusterList = clusters.zipWithIndex.flatMap {
      // case (cluster, index) if cluster.size > 3 => {
      case (clusterMembers, index) if clusterMembers.size > 1 => {
        clusterMembers.foreach(tab => {
          clusterIndex(tab.hashCode()) = index
        })

        logger.debug(s"> Cluster $index contains ${clusterMembers.toString()}")

        List(clusterMembers)
      }
      case _ => List()
    }

    logger.debug(
      s"> Computed overall index $clusterIndex for ${clusterList.length} clusters"
    )

    (clusterIndex, clusterList)
  }

  def updateManualClusters(manualGroups: List[TabGroup]): Unit = {
    val groupsAsTabSets = manualGroups.map(tabGroup => tabGroup.tabs.toSet)

    manualClusters.synchronized {
      manualClusters = processClusters(groupsAsTabSets)
    }
  }

  def clusters = {
    (
      manualClusters._1 ++ automatedClusters._1,
      manualClusters._2 ++ automatedClusters._2
    )
  }
}
