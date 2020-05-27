package heuristics

import scala.collection.JavaConverters._
import scalax.collection.Graph
import scalax.collection.edge.WDiEdge
import scalax.collection.io.dot._
import scalax.collection.edge.Implicits._
import scala.collection.mutable
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.SimpleWeightedGraph
import org.nlpub.watset.graph.SimplifiedWatset
import com.typesafe.scalalogging.LazyLogging
import scala.util.Try
import org.nlpub.watset.graph.NodeWeighting
import org.nlpub.watset.graph.ChineseWhispers
import org.nlpub.watset.graph.MaxMax
import org.nlpub.watset.graph.MarkovClustering
import java.{util => ju}

import tabstate.Tabs
import tabstate.Tab

object Watset extends App with LazyLogging {
  def apply(
      graph: Graph[Tab, WDiEdge]
  ): List[Set[Tab]] = {
    if (graph == null) {
      return List()
    }

    val watsetGraph = buildWatsetGraph(graph)

    if (watsetGraph
          .vertexSet()
          .size() < 2 || watsetGraph.edgeSet().size() == 0) {
      return List()
    }

    val clusters = computeClustersMarkov(watsetGraph)

    clusters.map(cluster => cluster.asScala.toSet)
  }

  def buildWatsetGraph(
      graph: Graph[Tab, WDiEdge]
  ): SimpleWeightedGraph[Tab, DefaultWeightedEdge] = {
    val graphBuilder =
      SimpleWeightedGraph
        .createBuilder[Tab, DefaultWeightedEdge](
          classOf[DefaultWeightedEdge]
        )

    graph.nodes.foreach(node => {
      graphBuilder.addVertex(node.value)
    })

    graph.edges.foreach(edge => {
      graphBuilder.addEdge(edge.from.value, edge.to.value, edge.weight)
    })

    val newGraph = graphBuilder.build()
    logger.debug(
      s"> Constructed watset graph with ${newGraph.vertexSet().size()} vertices and ${newGraph.edgeSet().size()} edges"
    )

    newGraph
  }

  def computeClustersMarkov(
      graph: SimpleWeightedGraph[Tab, DefaultWeightedEdge]
  ): List[ju.Collection[Tab]] = {
    val markovClusters = new MarkovClustering(graph, 2, 2)
    markovClusters.fit()

    logger.debug(
      s"Clusters (markov): ${markovClusters.getClusters().toString()}"
    )

    markovClusters.getClusters().asScala.toList
  }
}
