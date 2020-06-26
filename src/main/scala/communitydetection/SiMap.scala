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
import network.core.Statistics
import network.extendedmapequation.CPMapStatistics
import network.extendedmapequation.Stationary

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

    val tuples = graph
      .edgeSet()
      .asScala
      .toArray
      .flatMap((edge) => {
        val source = graph.getEdgeSource(edge)
        val target = graph.getEdgeTarget(edge)
        val weight = graph.getEdgeWeight(edge).toFloat
        if (weight >= params.minWeight) {
          List(
            (
              index.getOrElseUpdate(source, index.size),
              index.getOrElseUpdate(target, index.size),
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
    // val groups = graph.decompose(detectedPartition)

    // compute partition statistics
    // val partitionStats = Statistics.partition(detectedPartition, graph)

    val eval = CPMap.reWeight(graph, detectedPartition)
    println(eval.transition)
    // val evalWithProbabilities = new Stationary(1)
    //   .visitProbabilities(eval, detectedPartition, params.tau)
    // println(evalWithProbabilities.inWeight.toList)

    val nodeStats = List(
      eval.inWeight,
      eval.outWeight
    ).transpose.map {
      case List(inWeight, outWeight) => NodeStatistics(inWeight, outWeight)
    }

    // construct a mapping from tab hashes to the assigned partition
    val groupAssignmentMapping = detectedPartition.zipWithIndex
      .zip(nodeStats)
      .map {
        case (tuple: (Int, Int), stats: NodeStatistics) =>
          (index.get(tuple._2), tuple._1, stats)
      }
      .flatMap {
        case (Some(tab), partition, stats) => Seq((tab, partition, stats))
        case (None, _, _)                  => Seq()
      }
      .groupMap(_._2)(tuple => (tuple._1, tuple._3))

    groupAssignmentMapping.values
      .map(_.toSet)
      .toList
      .flatMap(group => {
        List((group.map(_._1), CliqueStatistics(group.map(_._2))))
      })

  }

}
