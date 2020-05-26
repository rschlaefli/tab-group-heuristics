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
  ): (mutable.Map[Int, Int], List[Set[Tab]]) = {
    if (graph == null) {
      return (mutable.Map(), List())
    }

    val watsetGraph = buildWatsetGraph(graph)

    if (watsetGraph
          .vertexSet()
          .size() < 2 || watsetGraph.edgeSet().size() == 0) {
      return (mutable.Map(), List())
    }

    val markovClusters = computeClustersMarkov(watsetGraph)

    processClusters(markovClusters)
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
  ): ju.Collection[ju.Collection[Tab]] = {
    val markovClusters = new MarkovClustering(graph, 2, 2)
    markovClusters.fit()

    logger.debug(
      s"Clusters (markov): ${markovClusters.getClusters().toString()}"
    )

    markovClusters.getClusters()
  }

  def processClusters(
      clusters: ju.Collection[ju.Collection[Tab]]
  ): (mutable.Map[Int, Int], List[Set[Tab]]) = {
    // preapre an index for which tab is stored in which cluster
    val clusterIndex: mutable.Map[Int, Int] = mutable.Map[Int, Int]()

    // prepare a return container for the clusters
    val clusterList = clusters.asScala.toList.zipWithIndex.flatMap {
      // case (cluster, index) if cluster.size > 3 => {
      case (cluster, index) if cluster.size > 1 => {
        val clusterMembers = cluster.asScala.toSet

        clusterMembers.foreach(tab => {
          clusterIndex(tab.hashCode()) = index
        })

        logger.debug(s"> Cluster $index contains ${clusterMembers.toString()}")

        List(clusterMembers)
      }
      case _ => List()
    }

    logger.debug(
      s"> Computed overall index $clusterIndex for ${clusterList.length} clusters"
    )

    (clusterIndex, clusterList)
  }
}
