package tabswitches

import java.io.StringWriter

import scala.util.Failure
import scala.util.Try

import com.typesafe.scalalogging.LazyLogging
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.SimpleWeightedGraph
import org.jgrapht.nio.Attribute
import org.jgrapht.nio.DefaultAttribute
import org.jgrapht.nio.dot.DOTExporter

import collection.mutable
import collection.JavaConverters._

object GraphUtils extends LazyLogging {

  import TabSwitchActor.TabSwitchGraph

  def processSwitchMap(
      tabSwitchMap: Map[String, TabSwitchMeta]
  ): TabSwitchGraph = {

    val tabGraph =
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
        case _           =>
      }

    tabGraph
  }

  def exportToDot(graph: TabSwitchGraph): String = {
    logger.debug(s"Exporting tab switch graph")

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
    writer.toString()
  }
}
