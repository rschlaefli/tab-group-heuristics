package communitydetection

import scala.collection.JavaConverters._
import scala.collection.mutable

import com.typesafe.scalalogging.LazyLogging
import network.core.ConnectedComponents
import network.core.Graph
import network.core.ListMatrix
import network.extendedmapequation.CPMap
import network.optimization.CPMapParameters
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.SimpleWeightedGraph
import smile.math.MathEx._
import tabswitches.TabMeta
import tabswitches.TabSwitchActor

case class SiMapParams(
    /**
      * Probability of choosing to teleport instead of following the transition probability matrix
      * to guarantee convergence of G * p = p
      */
    tau: Float = 0.15.toFloat,
    /**
      * Start resolution to search for the best resolution
      */
    resStart: Float = 0.0001.toFloat,
    /**
      * End resolution
      */
    resEnd: Float = 0.05.toFloat,
    /**
      * Accuracy of the best solution, e.g. when accuracy is 0.1,
      * the solution is refined util this close to the best resolution found so far
      */
    resAcc: Float = 0.001.toFloat,
    /**
      * Process only the largest connected component
      */
    largestCC: Boolean = false
) extends CommunityDetectorParameters {

  def asCPMapParameters =
    new CPMapParameters(tau, false, false, 1, resStart, resEnd, resAcc)

}

object SiMap
    extends App
    with LazyLogging
    with CommunityDetector[(Map[TabMeta, Int], Graph), SiMapParams] {

  val testGraph = loadTestGraph

  val tabGroups = apply(testGraph, SiMapParams())

  override def prepareGraph(
      graph: TabSwitchActor.TabSwitchGraph,
      pageRank: Map[TabMeta, Double],
      params: SiMapParams
  ): (Map[TabMeta, Int], Graph) = {

    val index = mutable.Map[TabMeta, Int]()

    val tuples = graph
      .edgeSet()
      .asScala
      .map((edge) => {
        val source = graph.getEdgeSource(edge)
        val target = graph.getEdgeTarget(edge)
        val weight = graph.getEdgeWeight(edge).toFloat

        val enhancedSource = source.withPageRank(pageRank(source))
        val enhancedTarget = target.withPageRank(pageRank(target))

        (
          index.getOrElseUpdate(enhancedSource, index.size),
          index.getOrElseUpdate(enhancedTarget, index.size),
          weight
        )
      })
      .toArray

    val (rows, cols, values) = tuples.unzip3[Int, Int, Float]

    var listMatrix = new ListMatrix()
      .init(rows, cols, values, true)
      .symmetrize()
      .sort()

    if (params.largestCC) {
      // FIXME: normalization issues
      val largestConnectedComponent =
        new ConnectedComponents(new Graph(listMatrix))
          .find()
          .getLargestComponent()

      val Array(_, largestComponent) = listMatrix
        .decompose(largestConnectedComponent)

      listMatrix = largestComponent.normalize()
    }

    (
      index.toMap,
      new Graph(listMatrix)
    )
  }

  override def computeGroups(
      graphAndIndex: (Map[TabMeta, Int], Graph),
      params: SiMapParams
  ): List[(Set[TabMeta], CliqueStatistics)] = {

    // swap keys and values of the index
    // this allows us to lookup tab hashes by the node id
    val index = graphAndIndex._1.map(_.swap)

    // compute the partitioning
    // returns a 1-D array with index=nodeId and value=partitionId
    val detectedPartition = CPMap
      .detect(graphAndIndex._2, params.asCPMapParameters)

    val groups = decomposeGraph(graphAndIndex._2, detectedPartition)

    val tabGroups = mapGraphsToTabGroups(groups, index)

    val tabGroupsNormalized = CliqueStatistics.normalize(tabGroups)

    tabGroupsNormalized.toList

  }

  /**
    * Decompose the graph into partitions (i.e., subgraphs)
    *
    * @param graph
    * @param partition
    * @return
    */
  def decomposeGraph(
      graph: Graph,
      partition: Array[Int]
  ): Array[(SimpleWeightedGraph[Int, DefaultWeightedEdge], Double)] = {
    graph
      .decompose(partition)
      .map(group => {
        val groupGraph = new SimpleWeightedGraph[Int, DefaultWeightedEdge](
          classOf[DefaultWeightedEdge]
        )

        val rows = group.getRows().toList
        val columns = group.getColumns().toList
        val values = group.getValues().toList

        List(rows, columns, values).transpose.foreach {
          case List(nodeId1: Int, nodeId2: Int, weight: Float) => {
            groupGraph.addVertex(nodeId1)
            groupGraph.addVertex(nodeId2)
            groupGraph.addEdge(nodeId1, nodeId2)
            groupGraph.setEdgeWeight(nodeId1, nodeId2, weight)
          }
        }

        (groupGraph, mean(group.getValues()))
      })
  }

  /**
    * Map partition graphs to tab groups with auxiliary statistics
    *
    * @param groups
    * @param index
    * @return
    */
  def mapGraphsToTabGroups(
      groups: Array[(SimpleWeightedGraph[Int, DefaultWeightedEdge], Double)],
      index: Map[Int, TabMeta]
  ): Array[(Set[TabMeta], CliqueStatistics)] = {

    groups.flatMap {
      case (groupGraph, meanWeight) if groupGraph.vertexSet.size > 1 =>
        val tabGroup = groupGraph
          .vertexSet()
          .asScala
          .map(nodeId => index(nodeId))
          .toSet

        val stats =
          CliqueStatistics(
            averageWeight = meanWeight,
            connectedness = computeConnectedness(groupGraph),
            pageRank = median(tabGroup.flatMap(_.pageRank).toArray)
          )

        Seq((tabGroup, stats))

      case _ => Seq()
    }

  }

  /**
    * Compute the Krackhardt connectedness of a graph
    *
    * @param graph
    * @return
    */
  def computeConnectedness(
      graph: SimpleWeightedGraph[_ <: Any, DefaultWeightedEdge]
  ): Double = {
    val numNodes = graph.vertexSet().size()

    val numEdges = graph.edgeSet().size()
    val maxPossibleEdges = (numNodes * (numNodes - 1)) / 2

    numEdges.doubleValue / maxPossibleEdges.doubleValue()
  }
}
