package refactor

import akka.actor.Actor
import akka.actor.ActorLogging
import org.jgrapht.graph.SimpleWeightedGraph
import org.jgrapht.graph.DefaultWeightedEdge
import scala.util.Success
import akka.pattern.{ask, pipe}
import scala.language.postfixOps
import org.jgrapht.nio.dot.DOTExporter
import java.io.StringWriter
import scala.concurrent.duration._
import scala.collection.mutable
import collection.JavaConverters._
import akka.util.Timeout
import org.jgrapht.nio.Attribute
import org.jgrapht.nio.DefaultAttribute

import SwitchMapActor.QueryTabSwitchMap
import graph.TabMeta
import graph.TabSwitchMeta
import scala.util.Failure
import akka.actor.Timers
import refactor.TabSwitchActor.CurrentSwitchGraph

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

            self ! ExportGraph(tabSwitchGraph)

            CurrentSwitchGraph(tabSwitchGraph)
          }
        }
        .pipeTo(sender())
    }

    case ExportGraph(graph) => {
      val exporter =
        new DOTExporter[TabMeta, DefaultWeightedEdge](_.hashCode().toString())
      exporter.setVertexAttributeProvider((tabMeta) => {
        val map = mutable.Map[String, Attribute]()
        map.put("label", DefaultAttribute.createAttribute(tabMeta.title))
        map.asJava
      })
      exporter.setEdgeAttributeProvider((edge) => {
        val map = mutable.Map[String, Attribute]()
        val weight = DefaultAttribute.createAttribute(graph.getEdgeWeight(edge))
        map.put("label", weight)
        map.put("weight", weight)
        map.asJava
      })

      val writer = new StringWriter()
      exporter.exportGraph(graph, writer)

      Persistence.persistString("tab_switches.dot", writer.toString())
    }

    case message =>
  }

}

object SwitchGraphActor {
  case class CurrentSwitchMap(switchMap: Map[String, TabSwitchMeta])
  case class ExportGraph(
      graph: SimpleWeightedGraph[TabMeta, DefaultWeightedEdge]
  )

  case object ComputeGraph

  def processSwitchMap(
      tabSwitchMap: Map[String, TabSwitchMeta]
  ): SimpleWeightedGraph[TabMeta, DefaultWeightedEdge] = {

    // compute a tab graph from the tab switch map
    var tabGraph =
      new SimpleWeightedGraph[TabMeta, DefaultWeightedEdge](
        classOf[DefaultWeightedEdge]
      )

    tabSwitchMap.values.foreach((switchData: TabSwitchMeta) => {
      tabGraph.addVertex(switchData.tab1)
      tabGraph.addVertex(switchData.tab2)
      tabGraph.addEdge(switchData.tab1, switchData.tab2)
      tabGraph
        .setEdgeWeight(switchData.tab1, switchData.tab2, switchData.count)
    })

    tabGraph
  }
}
