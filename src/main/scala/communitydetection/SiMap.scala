package communitydetection

import scala.collection.JavaConverters._
import scala.collection.mutable

import com.typesafe.scalalogging.LazyLogging
import network.core.Graph
import network.core.ListMatrix
import network.extendedmapequation.CPMap
import network.optimization.CPMapParameters
import tabswitches.TabMeta
import tabswitches.TabSwitchActor
import network.core.Statistics
import network.core.ConnectedComponents

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
    largestCC: Boolean = true,
    /**
      * Ignore edges with a lower weight
      */
    minWeight: Int = 2,
    /**
      * Remove groups with less nodes
      */
    minGroupSize: Int = 3,
    /**
      * Remove groups with more nodes
      */
    maxGroupSize: Int = 10,
    /**
      * The maximum number of groups to return
      */
    maxGroups: Int = 20
) extends Parameters {

  def asCPMapParameters =
    new CPMapParameters(tau, false, false, 1, resStart, resEnd, resAcc)

}

object SiMap
// extends App
    extends LazyLogging
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
          // println(source, target, weight)
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

    (
      index.toMap,
      new ListMatrix().init(rows, cols, values, true)
    )
  }

  override def computeGroups(
      matrixAndIndex: (Map[TabMeta, Int], ListMatrix),
      params: SiMapParams
  ): List[Set[TabMeta]] = {

    // swap keys and values of the index
    // this allows us to lookup tab hashes by the node id
    val index = matrixAndIndex._1.map(_.swap)

    // preprocess the input matrix and use it to construct a graph
    val preparedMatrix = matrixAndIndex._2.symmetrize().sort()
    // println(preparedMatrix)

    val graph = new Graph(preparedMatrix)

    if (params.largestCC) {
      val connectedComponents = new ConnectedComponents(graph).find()
      val largestComponent = connectedComponents.getLargestComponent()
      // println(largestComponent.sum)
    }

    // compute the partitioning
    // returns a 1-D array with index=nodeId and value=partitionId
    val detectedPartition = CPMap.detect(graph, params.asCPMapParameters)

    // compute the quality of the generated partitioning
    val quality =
      CPMap.evaluate(graph, detectedPartition, params.asCPMapParameters)
    logger.info(s"overall quality $quality")
    // println(quality)

    // decompose the graph into groups
    val groups = graph.decompose(detectedPartition)
    // groups.foreach(println)

    // fold the graph
    val folded = graph.fold(detectedPartition)
    // println(folded)

    // compute partition statistics
    val partitionStats = Statistics.partition(detectedPartition, preparedMatrix)
    // println(partitionStats)

    val arrayStats = Statistics.array(detectedPartition)
    // println(arrayStats)

    // construct a mapping from tab hashes to the assigned partition
    val groupAssignmentMapping = detectedPartition.zipWithIndex
      .map(tuple => (index.get(tuple._2), tuple._1))
      .flatMap {
        case (Some(tab), partition) => Seq((tab, partition))
        case (None, _)              => Seq()
      }
      .groupMap(_._2)(_._1)

    // transform the arrays
    groupAssignmentMapping.values.map(_.toSet).toList

  }

  override def processGroups(
      tabGroups: List[Set[TabMeta]],
      params: SiMapParams
  ): List[Set[TabMeta]] = {
    val filteredGroups = tabGroups.filter(group =>
      params.maxGroupSize >= group.size
        && group.size >= params.minGroupSize
    )
    // println(filteredGroups)
    filteredGroups.take(params.maxGroups)
  }

}
