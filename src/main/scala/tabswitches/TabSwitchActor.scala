package tabswitches

import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import communitydetection._
import heuristics.HeuristicsActor.TabSwitchHeuristicsResults
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultWeightedEdge
import org.slf4j.MarkerFactory
import tabstate.Tab

import SwitchMapActor.ProcessTabSwitch
import SwitchGraphActor.ComputeGraph

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
          switchMap ! SwitchMapActor.ProcessTabSwitch(prevTab, newTab)
          temp = None
        }
        // process a switch to an irrelevant tab
        case (false, true) => {
          temp = Some(prevTab)
        }
        // process a switch from an irrelevant tab
        case (true, false) => {
          if (temp.isDefined)
            switchMap ! SwitchMapActor.ProcessTabSwitch(temp.get, newTab)
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
            Watset(graph, WatsetParams(), "clusters_watset.txt")

            val simapClusters =
              SiMap(graph, SiMapParams(), "clusters_simap.txt")

            val (clusterIndex, clusters) =
              buildClusterIndexWithStats(simapClusters)

            TabSwitchHeuristicsResults(clusterIndex, clusters)
          }
        }
        .pipeTo(sender())
    }

    case message => log.debug(s"Received message $message")
  }
}

object TabSwitchActor extends LazyLogging {

  type TabSwitchGraph = Graph[TabMeta, DefaultWeightedEdge]

  case object ComputeGroups

  case class TabSwitch(tab1: Option[Tab], tab2: Tab)
  case class CurrentSwitchGraph(graph: TabSwitchGraph)

  def buildClusterIndexWithStats(
      clusters: List[(Set[TabMeta], CliqueStatistics)]
  ): (Map[Int, Int], List[Set[TabMeta]]) = {
    buildClusterIndex(clusters.map(_._1))
  }

  def buildClusterIndex(
      clusters: List[Set[TabMeta]]
  ): (Map[Int, Int], List[Set[TabMeta]]) = {
    // prepare an index for which tab is stored in which cluster
    val clusterIndex = mutable.Map[Int, Int]()

    // prepare a return container for the clusters
    val clusterList = clusters.zipWithIndex
      .flatMap {
        case (clusterMembers, index) if clusterMembers.size > 1 => {
          clusterMembers.foreach(tab => {
            clusterIndex(tab.hashCode()) = index
          })

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
