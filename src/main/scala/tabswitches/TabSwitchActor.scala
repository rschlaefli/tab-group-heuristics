package tabswitches

import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import heuristics.HeuristicsActor.TabSwitchHeuristicsResults
import heuristics.KeywordExtraction
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.SimpleWeightedGraph
import org.slf4j.MarkerFactory
import tabstate.Tab
import util.Utils

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
            val automatedClusters = Utils.processClusters(computedClusters)

            // generating cluster titles
            log.debug(s"> Generating tab cluster titles")

            val clustersWithTitles = automatedClusters._2.map(tabCluster => {
              val keywords = KeywordExtraction(tabCluster)
              (keywords.mkString(" "), tabCluster)
            })
            clustersWithTitles.map(_._1)

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
}
