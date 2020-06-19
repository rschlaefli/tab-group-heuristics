package tabswitches

import scala.collection.JavaConverters._
import scala.collection.mutable
import org.jgrapht.Graph
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
import org.jgrapht.graph.DefaultUndirectedWeightedGraph

object Watset extends App with LazyLogging {
  def apply(
      graph: Graph[TabMeta, DefaultWeightedEdge]
  ): List[Set[TabMeta]] = {
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
      graph: Graph[TabMeta, DefaultWeightedEdge]
  ): Graph[TabMeta, DefaultWeightedEdge] = {
    graph
  }

  def computeClustersMarkov(
      graph: Graph[TabMeta, DefaultWeightedEdge]
  ): List[ju.Collection[TabMeta]] = {
    val markovClusters = new MarkovClustering(graph, 2, 2)
    markovClusters.fit()

    logger.debug(
      s"Clusters (markov): ${markovClusters.getClusters().toString()}"
    )

    markovClusters.getClusters().asScala.toList
  }
}
