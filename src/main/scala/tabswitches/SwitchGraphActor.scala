package tabswitches

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Failure
import scala.util.Try

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.SimpleWeightedGraph
import persistence.Persistence

import SwitchMapActor.QueryTabSwitchMap
import TabSwitchActor.CurrentSwitchGraph

case class GraphGenerationParams(
    sameOriginFactor: Double = 0.9,
    urlSimilarityFactor: Double = 0.1
)

class SwitchGraphActor extends Actor with ActorLogging {

  import SwitchGraphActor._

  implicit val executionContext = context.dispatcher

  override def receive: Actor.Receive = {

    case ComputeGraph => {
      implicit val timeout = Timeout(2 seconds)
      val switchMap = context.actorSelection(
        "/user/Main/Heuristics/TabSwitches/TabSwitchMap"
      )
      (switchMap ? QueryTabSwitchMap)
        .mapTo[CurrentSwitchMap]
        .map {
          case CurrentSwitchMap(tabSwitchMap) => {
            log.debug(
              s"Constructing tab switch graph from switch map with ${tabSwitchMap.size} entries"
            )

            val tabSwitchGraph = processSwitchMap(tabSwitchMap)

            log.debug(
              s"Contructed tab switch graph with ${tabSwitchGraph.vertexSet().size()}" +
                s"nodes and ${tabSwitchGraph.edgeSet().size()} edges"
            )

            context.actorSelection(
              "/user/Main/Heuristics/TabSwitches/TabSwitchGraph"
            ) ! ExportGraph(tabSwitchGraph)

            CurrentSwitchGraph(tabSwitchGraph)
          }
        }
        .pipeTo(sender())
    }

    case ExportGraph(graph) => {
      val dotString = GraphExport.toDot(graph)
      val csvString = GraphExport.toCsv(graph)
      Persistence.persistString("tab_switches.dot", dotString)
      Persistence.persistString("tab_switches.txt", csvString)
    }

    case _ =>
  }

}

object SwitchGraphActor extends LazyLogging {

  import TabSwitchActor.TabSwitchGraph

  case object ComputeGraph

  case class CurrentSwitchMap(switchMap: Map[String, TabSwitchMeta])
  case class ExportGraph(graph: TabSwitchGraph)

  def processSwitchMap(
      tabSwitchMap: Map[String, TabSwitchMeta],
      params: GraphGenerationParams = GraphGenerationParams()
  ): TabSwitchGraph = {

    val tabGraph =
      new SimpleWeightedGraph[TabMeta, DefaultWeightedEdge](
        classOf[DefaultWeightedEdge]
      )

    tabSwitchMap.values
      .filter(switch => switch.tab1 != switch.tab2)
      .map((switchData: TabSwitchMeta) =>
        Try {
          tabGraph.addVertex(switchData.tab1)
          tabGraph.addVertex(switchData.tab2)
          tabGraph.addEdge(switchData.tab1, switchData.tab2)

          val weightedCount =
            (switchData.sameOrigin, switchData.urlSimilarity) match {
              case (Some(true), Some(urlSimilarity)) =>
                switchData.count * params.sameOriginFactor * (1 - params.urlSimilarityFactor * urlSimilarity)
              case (Some(true), _) =>
                switchData.count * params.sameOriginFactor
              case (_, Some(urlSimilarity)) =>
                switchData.count * (1 - urlSimilarity)
              case _ =>
                switchData.count
            }

          tabGraph
            .setEdgeWeight(switchData.tab1, switchData.tab2, weightedCount)

        }
      )
      .filter(_.isFailure)
      .foreach {
        case Failure(ex) => logger.error(ex.getMessage())
        case _           =>
      }

    tabGraph
  }
}
