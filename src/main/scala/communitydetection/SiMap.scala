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
    with CommunityDetector[ListMatrix, SiMapParams] {

  val testGraph = loadTestGraph

  val tabGroups = apply(testGraph, SiMapParams())

  override def prepareGraph(
      graph: TabSwitchActor.TabSwitchGraph
  ): ListMatrix = {

    val index = mutable.Map[String, Int]()

    val tuples = graph
      .edgeSet()
      .asScala
      .toArray
      .map((edge) => {
        val source = graph.getEdgeSource(edge)
        val target = graph.getEdgeTarget(edge)
        val weight = graph.getEdgeWeight(edge).toFloat
        (
          index.getOrElseUpdate(source.hash, index.size + 1),
          index.getOrElseUpdate(target.hash, index.size + 1),
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

    new ListMatrix().init(rows, cols, values, true)
  }

  override def computeGroups(
      listMatrix: ListMatrix,
      params: SiMapParams
  ): List[Set[TabMeta]] = {

    Shared.setVerbose(true)

    val graph = new Graph(listMatrix.symmetrize().sort().normalize());
    val siGraph = new SiGraph(graph)

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

    val detectedPartition = CPMap.detect(graph, cpMapParams)
    println(detectedPartition)

    GraphIO.writePartition(siGraph, detectedPartition, "partition_out.txt");

    List()

  }

  override def processGroups(
      tabGroups: List[Set[TabMeta]]
  ): List[Set[TabMeta]] = tabGroups

}
