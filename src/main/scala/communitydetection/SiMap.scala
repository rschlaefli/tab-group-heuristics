package communitydetection

import scala.collection.JavaConverters._
import scala.collection.mutable

import com.typesafe.scalalogging.LazyLogging
import network.core.Graph
import network.core.GraphIO
import network.core.ListMatrix
import network.core.SiGraph
import tabswitches.TabMeta
import tabswitches.TabSwitchActor
import network.optimization.CPMapParameters
import network.extendedmapequation.CPMap
import persistence.Persistence
import network.Shared

case class SiMapParams() extends Parameters

object SiMap
    extends App
    with LazyLogging
    with CommunityDetector[(Map[TabMeta, Int], ListMatrix), SiMapParams] {

  val testGraph = loadTestGraph

  val tabGroups = apply(testGraph, SiMapParams())

  logger.debug(s"Clusters (SiMap): ${tabGroups}")

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

    Persistence.persistString(
      "raw_input.txt",
      tuples
        .map(tuple => s"${tuple._1}\t${tuple._2}\t${tuple._3}")
        .mkString("\n")
    )

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

    // Shared.setVerbose(true)

    // swap keys and values of the index
    // this allows us to lookup tab hashes by the node id
    val index = matrixAndIndex._1.map(_.swap)

    val graph = new Graph(matrixAndIndex._2.symmetrize().sort().normalize());
    // val siGraph = new SiGraph(graph)

    val cpMapParams =
      new CPMapParameters(
        0.15.floatValue(),
        false,
        false,
        1,
        0.001.floatValue(),
        0.05.floatValue(),
        0.002.floatValue()
      )

    // compute the partitioning
    // returns a 1-D array with index=nodeId and value=partitionId
    val detectedPartition = CPMap.detect(graph, cpMapParams)
    // GraphIO.writePartition(siGraph, detectedPartition, "partition_out.txt");

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
