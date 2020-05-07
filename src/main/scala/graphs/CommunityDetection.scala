package graphs

import scalax.collection.Graph
import scalax.collection.edge.WDiEdge
import scalax.collection.io.dot._
import scalax.collection.edge.Implicits._
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.SimpleWeightedGraph
import org.nlpub.watset.graph.SimplifiedWatset

import tabstate.Tabs
import tabstate.Tab
import heuristics.TabSwitches
import com.typesafe.scalalogging.LazyLogging

object CommunityDetection extends App with LazyLogging {
  val watsetGraph =
    SimpleWeightedGraph.createBuilder[Tabs, DefaultWeightedEdge](
      classOf[DefaultWeightedEdge]
    )

  def processGraph(graph: Graph[Tabs, WDiEdge]) = {
    logger.info("> processing the graph")

    // graph.nodes.foreach(node => {
    //   watsetGraph.addVertex(node.value)
    // })

    // graph.edges.foreach(edge => {
    //   watsetGraph.addEdge(edge.from.value, edge.to.value, edge.weight)
    // })

    // watsetGraph.build()

    // logger.info(watsetGraph.toString())
  }
}
