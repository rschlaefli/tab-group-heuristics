package communitydetection

import scala.collection.JavaConverters._
import scala.collection.mutable

import com.typesafe.scalalogging.LazyLogging
import network.core.ConnectedComponents
import network.core.Graph
import network.core.ListMatrix
import network.extendedmapequation.CPMap
import network.optimization.CPMapParameters
import tabswitches.TabMeta
import tabswitches.TabSwitchActor
import org.jgrapht.graph.SimpleWeightedGraph
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.alg.scoring.PageRank

case class SiMapParams(
    /**
      * Probability of choosing to teleport instead of following the transition probability matrix
      * to guarantee convergence of G * p = p
      */
    tau: Float = 0.15.toFloat,
    /**
      * Start resolution to search for the best resolution
      */
    resStart: Float = 0.001.toFloat,
    /**
      * End resolution
      */
    resEnd: Float = 0.05.toFloat,
    /**
      * Accuracy of the best solution, e.g. when accuracy is 0.1,
      * the solution is refined util this close to the best resolution found so far
      */
    resAcc: Float = 0.002.toFloat,
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
    with CommunityDetector[(Map[TabMeta, Int], ListMatrix), SiMapParams] {

  val testGraph = loadTestGraph

  val tabGroups = apply(testGraph, SiMapParams())

  override def prepareGraph(
      graph: TabSwitchActor.TabSwitchGraph,
      params: SiMapParams
  ): (Map[TabMeta, Int], ListMatrix) = {

    val index = mutable.Map[TabMeta, Int]()

    val pageRank = new PageRank(graph)

    val tuples = graph
      .edgeSet()
      .asScala
      .toArray
      .flatMap((edge) => {
        val source = graph.getEdgeSource(edge)
        val target = graph.getEdgeTarget(edge)
        val weight = graph.getEdgeWeight(edge).toFloat
        if (weight >= params.minWeight) {
          val enhancedSource = source
            .withPageRank(pageRank.getVertexScore(source).toDouble)
          val enhancedTarget = target
            .withPageRank(pageRank.getVertexScore(target).toDouble)
          List(
            (
              index.getOrElseUpdate(enhancedSource, index.size),
              index.getOrElseUpdate(enhancedTarget, index.size),
              weight
            )
          )
        } else {
          List()
        }
      })

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
      listMatrix
    )
  }

  override def computeGroups(
      matrixAndIndex: (Map[TabMeta, Int], ListMatrix),
      params: SiMapParams
  ): List[(Set[TabMeta], CliqueStatistics)] = {

    // swap keys and values of the index
    // this allows us to lookup tab hashes by the node id
    val index = matrixAndIndex._1.map(_.swap)

    // construct a graph
    val graph = new Graph(matrixAndIndex._2)

    // compute the partitioning
    // returns a 1-D array with index=nodeId and value=partitionId
    val detectedPartition = CPMap.detect(graph, params.asCPMapParameters)

    // compute the quality of the generated partitioning
    val quality = CPMap
      .evaluate(graph, detectedPartition, params.asCPMapParameters)
    logger.info(s"overall quality $quality")

    // decompose the graph into groups
    val groups = graph
      .decompose(detectedPartition)
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

        groupGraph
      })

    val tabGroups = groups
      .map(group => {

        val tabGroup = group
          .vertexSet()
          .asScala
          .map(nodeId => index(nodeId))
          .toSet

        val stats = CliqueStatistics(tabGroup.flatMap(_.pageRank).sum)

        (tabGroup, stats)
      })
      .toList

    tabGroups

  }

}
