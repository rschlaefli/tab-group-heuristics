package communitydetection

import scala.collection.JavaConverters._
import com.typesafe.scalalogging.LazyLogging
import tabswitches.TabMeta
import tabswitches.TabSwitchActor
import network.extendedmapequation.CPMap
import network.optimization.CPMapParameters
import network.core.GraphIO
import network.core.SiGraph
import network.optimization.CPM
import network.core.Graph
import network.core.ListMatrix
import scala.collection.mutable

case class SiMapParams() extends Parameters

object SiMap
    extends LazyLogging
    with CommunityDetector[ListMatrix, SiMapParams] {

  override def prepareGraph(
      graph: TabSwitchActor.TabSwitchGraph
  ): ListMatrix = {

    val index = mutable.Map[String, Int]()

    val unzipped: (Array[Int], Array[Int], Array[Float]) = graph
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
      .unzip3[Int, Int, Float]
    new ListMatrix().init(unzipped._1, unzipped._2, unzipped._3, true)
  }

  override def computeGroups(
      listMatrix: ListMatrix,
      params: SiMapParams
  ): List[Set[TabMeta]] = {
    val graph = new Graph(listMatrix.sort().normalize());
    val siGraph = new SiGraph(graph)

    // val cpmap = new CPMap()
    // val cpmapParams =
    //   new CPMapParameters(
    //     0.15.floatValue(),
    //     false,
    //     false,
    //     4,
    //     0.001.floatValue(),
    //     0.05.floatValue(),
    //     0.002.floatValue()
    //   )

    val cpm: CPM = new CPM(0.002.floatValue())
      .setThreadCount(1)
      .asInstanceOf[CPM]

    val partition = cpm.detect(siGraph)

    GraphIO.writePartition(siGraph, partition, "graph_output.txt")

    List()
  }

  override def processGroups(
      tabGroups: List[Set[TabMeta]]
  ): List[Set[TabMeta]] = tabGroups

}
