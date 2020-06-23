package tabswitches

import akka.actor.Actor
import akka.actor.ActorLogging
import org.jgrapht.graph.SimpleWeightedGraph
import org.jgrapht.graph.DefaultWeightedEdge
import akka.pattern.{ask, pipe}
import scala.language.postfixOps
import scala.concurrent.duration._
import akka.util.Timeout
import scala.util.Failure
import com.typesafe.scalalogging.LazyLogging
import scala.util.Try

import persistence.Persistence
import SwitchMapActor.QueryTabSwitchMap
import TabSwitchActor.CurrentSwitchGraph

class SwitchGraphActor extends Actor with ActorLogging {

  import SwitchGraphActor._

  implicit val executionContext = context.dispatcher

  override def receive: Actor.Receive = {

    case ComputeGraph => {
      implicit val timeout = Timeout(2 seconds)
      val switchMap = context.actorSelection(
        "/user/Heuristics/TabSwitches/TabSwitchMap"
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
              "/user/Heuristics/TabSwitches/TabSwitchGraph"
            ) ! ExportGraph(tabSwitchGraph)

            CurrentSwitchGraph(tabSwitchGraph)
          }
        }
        .pipeTo(sender())
    }

    case ExportGraph(graph) => {
      val dotString = GraphUtils.exportToDot(graph)
      Persistence.persistString("tab_switches.dot", dotString)
    }

    case message =>
  }

}

object SwitchGraphActor extends LazyLogging {
  case object ComputeGraph

  case class CurrentSwitchMap(switchMap: Map[String, TabSwitchMeta])
  case class ExportGraph(
      graph: SimpleWeightedGraph[TabMeta, DefaultWeightedEdge]
  )

  def processSwitchMap(
      tabSwitchMap: Map[String, TabSwitchMeta]
  ): SimpleWeightedGraph[TabMeta, DefaultWeightedEdge] = {

    var tabGraph =
      new SimpleWeightedGraph[TabMeta, DefaultWeightedEdge](
        classOf[DefaultWeightedEdge]
      )

    tabSwitchMap.values
      .map((switchData: TabSwitchMeta) =>
        Try {
          tabGraph.addVertex(switchData.tab1)
          tabGraph.addVertex(switchData.tab2)
          tabGraph.addEdge(switchData.tab1, switchData.tab2)
          tabGraph
            .setEdgeWeight(switchData.tab1, switchData.tab2, switchData.count)
        }
      )
      .filter(_.isFailure)
      .foreach {
        case Failure(ex) => logger.error(ex.getMessage())
      }

    tabGraph
  }
}
