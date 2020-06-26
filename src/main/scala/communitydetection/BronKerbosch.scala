package communitydetection

import tabswitches.TabSwitchActor
import tabswitches.TabMeta
import org.jgrapht.alg.clique.BronKerboschCliqueFinder
import scala.collection.JavaConverters._

case class BronKerboschParams() extends CommunityDetectorParameters

object BronKerbosch
    extends App
    with CommunityDetector[TabSwitchActor.TabSwitchGraph, BronKerboschParams] {

  val testGraph = loadTestGraph

  val tabGroups = apply(testGraph, BronKerboschParams())

  override def prepareGraph(
      graph: TabSwitchActor.TabSwitchGraph,
      params: BronKerboschParams
  ): TabSwitchActor.TabSwitchGraph = graph

  override def computeGroups(
      graph: TabSwitchActor.TabSwitchGraph,
      params: BronKerboschParams
  ): List[(Set[TabMeta], CliqueStatistics)] = {

    val cliques = new BronKerboschCliqueFinder(graph)

    cliques.asScala
      .map(_.asScala.toSet)
      .toList
      .map((_, CliqueStatistics()))
  }

}
