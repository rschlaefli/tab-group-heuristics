package graph

import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import scala.collection.mutable
import org.jgrapht.graph._
import org.jgrapht.Graph
import scala.util.Try
import org.jgrapht.nio.dot.DOTExporter
import org.jgrapht.nio.Attribute
import org.jgrapht.nio.DefaultAttribute
import java.io.StringWriter
import collection.JavaConverters._

import tabstate.Tab
import persistence.Persistable
import scala.util.Success
import scala.util.Failure

object TabSwitchGraph extends LazyLogging {

  def apply(): Graph[TabMeta, DefaultWeightedEdge] = {

    val tabSwitchMap = TabSwitchMap.tabSwitches

    logger.debug(
      s"> Constructing tab switch graph from switch map with ${tabSwitchMap.size} entries"
    )

    var tabGraph =
      new SimpleWeightedGraph[TabMeta, DefaultWeightedEdge](
        classOf[DefaultWeightedEdge]
      )

    tabSwitchMap.values.foreach((switchData: TabSwitchMeta) => {
      tabGraph.addVertex(switchData.tab1)
      tabGraph.addVertex(switchData.tab2)
      tabGraph.addEdge(switchData.tab1, switchData.tab2)
      tabGraph.setEdgeWeight(switchData.tab1, switchData.tab2, switchData.count)
    })

    logger.debug(
      s"> Contructed tab switch graph with ${tabGraph.vertexSet().size()} nodes and ${tabGraph.edgeSet().size()} edges"
    )

    exportGraph(tabGraph) match {
      case Failure(exception) =>
        logger.error(s"[TabSwitchGraph] ${exception.getMessage()}")
      case _: Try[_] =>
    }

    tabGraph
  }

  def exportGraph(
      graph: Graph[TabMeta, DefaultWeightedEdge]
  ): Try[Unit] = Try {
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

    Persistable.persistString(
      "tab_switches.dot",
      writer.toString()
    )
  }
}
