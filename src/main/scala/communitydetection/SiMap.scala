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
    with CommunityDetector[(Map[String, Int], ListMatrix), SiMapParams] {

  val testGraph = loadTestGraph

  val tabGroups = apply(testGraph, SiMapParams())

  override def prepareGraph(
      graph: TabSwitchActor.TabSwitchGraph
  ): (Map[String, Int], ListMatrix) = {

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

    (index.toMap, new ListMatrix().init(rows, cols, values, true))
  }

  override def computeGroups(
      matrixAndIndex: (Map[String, Int], ListMatrix),
      params: SiMapParams
  ): List[Set[TabMeta]] = {

    Shared.setVerbose(true)

    // swap keys and values of the index
    // this allows us to lookup tab hashes by the node id
    val index = matrixAndIndex._1.map(_.swap)

    val graph = new Graph(matrixAndIndex._2.symmetrize().sort().normalize());
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
    println(detectedPartition.toList)

    // arr.foreach(partition => {
    //   println(partition.toList.map(index.get))
    // })

    // TODO: build tab groups from the detected partitioning

    GraphIO.writePartition(siGraph, detectedPartition, "partition_out.txt");

    List()

  }

  override def processGroups(
      tabGroups: List[Set[TabMeta]]
  ): List[Set[TabMeta]] = tabGroups

}
