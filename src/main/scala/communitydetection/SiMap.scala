package communitydetection

import scala.collection.JavaConverters._
import scala.collection.mutable

import com.typesafe.scalalogging.LazyLogging
import network.core.Graph
import network.core.ListMatrix
import network.extendedmapequation.CPMap
import network.optimization.CPMapParameters
import persistence.Persistence
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
    resStart: Float = 0.001.toFloat,
    /**
      * End resolution
      */
    resEnd: Float = 0.05.toFloat,
    /**
      * Accuracy of the best solution, e.g. when accuracy is 0.1,
      * the solution is refined util this close to the best resolution found so far
      */
    resAcc: Float = 0.002.toFloat
) extends Parameters {

  def asCPMapParameters =
    new CPMapParameters(tau, false, false, 1, resStart, resEnd, resAcc)

}

object SiMap
    extends LazyLogging
    with CommunityDetector[(Map[TabMeta, Int], ListMatrix), SiMapParams] {

  val testGraph = loadTestGraph

  val tabGroups = apply(testGraph, SiMapParams())

  override def prepareGraph(
      graph: TabSwitchActor.TabSwitchGraph
  ): (Map[TabMeta, Int], ListMatrix) = {

    val index = mutable.Map[TabMeta, Int]()

    val tuples = graph
      .edgeSet()
      .asScala
      .toArray
      .map((edge) => {
        val source = graph.getEdgeSource(edge)
        val target = graph.getEdgeTarget(edge)
        val weight = graph.getEdgeWeight(edge).toFloat
        (
          index.getOrElseUpdate(source, index.size + 1),
          index.getOrElseUpdate(target, index.size + 1),
          weight
        )
      })

    // Persistence.persistString(
    //   "raw_input.txt",
    //   tuples
    //     .map(tuple => s"${tuple._1}\t${tuple._2}\t${tuple._3}")
    //     .mkString("\n")
    // )

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
    val graph = new Graph(matrixAndIndex._2.symmetrize().sort().normalize())

    // compute the partitioning
    // returns a 1-D array with index=nodeId and value=partitionId
    val detectedPartition = CPMap.detect(graph, params.asCPMapParameters)

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
      tabGroups: List[Set[TabMeta]]
  ): List[Set[TabMeta]] = {

    Persistence.persistString(
      "clusters_simap.txt",
      tabGroups.map(_.toString()).mkString("\n")
    )

    tabGroups
  }

}
