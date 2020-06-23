package communitydetection

import scala.collection.JavaConverters._
import scala.collection.mutable

import com.typesafe.scalalogging.LazyLogging
import network.core.Graph
import network.core.GraphIO
import network.core.ListMatrix
import network.core.SiGraph
import network.optimization.CPM
import tabswitches.TabMeta
import tabswitches.TabSwitchActor
import network.optimization.CPMapParameters
import network.extendedmapequation.CPMap
import persistence.Persistence

case class SiMapParams() extends Parameters

object SiMap
    extends LazyLogging
    with CommunityDetector[ListMatrix, SiMapParams] {

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

    // .filter(_._3 > 1)
    val unzipped: (Array[Int], Array[Int], Array[Float]) =
      tuples.unzip3[Int, Int, Float]

    new ListMatrix().init(unzipped._1, unzipped._2, unzipped._3, true)
  }

  override def computeGroups(
      listMatrix: ListMatrix,
      params: SiMapParams
  ): List[Set[TabMeta]] = {
    val graph = new Graph(listMatrix.sort().normalize());
    val siGraph = new SiGraph(graph)

    val cpMap = new CPMap()
    val cpMapParams =
      new CPMapParameters(
        0.15.floatValue(),
        false,
        false,
        4,
        0.001.floatValue(),
        0.05.floatValue(),
        0.002.floatValue()
      )

    // val cpm: CPM = new CPM(0.002.floatValue())
    //   .setThreadCount(2)

    // val partition = cpm.detect(siGraph)

    // println(partition)
    // GraphIO.writePartition(siGraph, partition, "graph_output.txt")
    List()
  }

  override def processGroups(
      tabGroups: List[Set[TabMeta]]
  ): List[Set[TabMeta]] = tabGroups

}
