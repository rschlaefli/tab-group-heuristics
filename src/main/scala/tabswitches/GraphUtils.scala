package tabswitches

import java.io.StringWriter

import scala.util.Failure
import scala.util.Try

import com.typesafe.scalalogging.LazyLogging
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.SimpleWeightedGraph
import org.jgrapht.nio.Attribute
import org.jgrapht.nio.DefaultAttribute
import org.jgrapht.nio.csv.CSVExporter
import org.jgrapht.nio.csv.CSVFormat
import org.jgrapht.nio.dot.DOTExporter

import collection.mutable
import collection.JavaConverters._

object GraphUtils extends LazyLogging {

  import TabSwitchActor.TabSwitchGraph

  def exportToCsv(graph: TabSwitchGraph): String = {
    val exporter =
      new CSVExporter[TabMeta, DefaultWeightedEdge](
        CSVFormat.EDGE_LIST
      )
    exporter.setParameter(CSVFormat.Parameter.EDGE_WEIGHTS, true)

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
