package tabswitches

import akka.actor.Actor
import akka.actor.ActorLogging
import com.typesafe.scalalogging.LazyLogging
import org.slf4j.MarkerFactory
import akka.actor.Props
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import scala.language.postfixOps
import scala.concurrent.duration._
import akka.util.Timeout
import scala.collection.mutable

import tabstate.Tab
import SwitchMapActor.ProcessTabSwitch
import SwitchGraphActor.ComputeGraph
import scala.util.Success
import org.jgrapht.graph.SimpleWeightedGraph
import org.jgrapht.graph.DefaultWeightedEdge
import scala.util.Failure
import heuristics.KeywordExtraction
import heuristics.HeuristicsActor.TabSwitchHeuristicsResults

class TabSwitchActor extends Actor with ActorLogging {

  import TabSwitchActor._

  implicit val executionContext = context.dispatcher

  val switchMap = context.actorOf(Props[SwitchMapActor], "TabSwitchMap")
  val switchGraph = context.actorOf(Props[SwitchGraphActor], "TabSwitchGraph")

  val logToCsv = MarkerFactory.getMarker("CSV")

  var temp: Option[Tab] = None

  override def receive: Actor.Receive = {
    case TabSwitch(Some(prevTab), newTab) => {
      val irrelevantTabs = List("New Tab", "Tab Groups")
      val switchFromIrrelevantTab = irrelevantTabs.contains(prevTab.title)
      val switchToIrrelevantTab = irrelevantTabs.contains(newTab.title)

      (switchFromIrrelevantTab, switchToIrrelevantTab) match {
        // process a normal tab switch
        case (false, false) if prevTab.hash != newTab.hash => {
          switchMap ! ProcessTabSwitch(prevTab, newTab)
          temp = None
        }
        // process a switch to an irrelevant tab
        case (false, true) => {
          temp = Some(prevTab)
        }
        // process a switch from an irrelevant tab
        case (true, false) => {
          if (temp.isDefined) switchMap ! ProcessTabSwitch(temp.get, newTab)
          temp = None
        }
        case _ =>
          log.debug(s"Ignored switch from $prevTab to $newTab")
      }
    }

    case ComputeGroups => {
      implicit val timeout = Timeout(10 seconds)
      (switchGraph ? ComputeGraph)
        .mapTo[CurrentSwitchGraph]
        .map {
          case CurrentSwitchGraph(graph) => {
            log.debug(s"Computing tab clusters")

            val computedClusters = Watset(graph)
            val automatedClusters = processClusters(computedClusters)

            // generating cluster titles
            log.debug(s"> Generating tab cluster titles")

            val clustersWithTitles = automatedClusters._2.map(tabCluster => {
              val keywords = KeywordExtraction(tabCluster)
              (keywords.mkString(" "), tabCluster)
            })
            val automatedTitles = clustersWithTitles.map(_._1)

            log.info(clustersWithTitles.toString())

            TabSwitchHeuristicsResults(automatedClusters._1, clustersWithTitles)
          }
        }
        .pipeTo(sender())
    }

    case message => log.debug(s"Received message $message")
  }
}

object TabSwitchActor extends LazyLogging {
  case object ComputeGroups

  case class TabSwitch(tab1: Option[Tab], tab2: Tab)
  case class CurrentSwitchGraph(
      graph: SimpleWeightedGraph[TabMeta, DefaultWeightedEdge]
  )

  def processClusters(
      clusters: List[Set[TabMeta]]
  ): (Map[Int, Int], List[Set[TabMeta]]) = {
    // preapre an index for which tab is stored in which cluster
    val clusterIndex = mutable.Map[Int, Int]()

    // prepare a return container for the clusters
    val clusterList = clusters.zipWithIndex.flatMap {
      case (clusterMembers, index) if clusterMembers.size > 1 => {
        clusterMembers.foreach(tab => {
          clusterIndex(tab.hashCode()) = index
        })

        logger.debug(s"Cluster $index contains ${clusterMembers.toString()}")

        List(clusterMembers)
      }
      case _ => List()
    }

    logger.debug(
      s"Computed overall index $clusterIndex for ${clusterList.length} clusters"
    )

    (Map.from(clusterIndex), clusterList)
  }
}
