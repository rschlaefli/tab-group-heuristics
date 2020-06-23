package tabswitches

import java.io.StringWriter

import com.typesafe.scalalogging.LazyLogging
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.SimpleWeightedGraph
import org.jgrapht.nio.Attribute
import org.jgrapht.nio.DefaultAttribute
import org.jgrapht.nio.dot.DOTExporter

import collection.mutable
import collection.JavaConverters._

object GraphUtils extends LazyLogging {
  def exportToDot(
      graph: SimpleWeightedGraph[TabMeta, DefaultWeightedEdge]
  ): String = {
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
