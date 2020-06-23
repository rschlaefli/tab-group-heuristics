package communitydetection

import com.typesafe.scalalogging.LazyLogging
import tabswitches.TabMeta
import tabswitches.TabSwitchActor

case class SiMapParams() extends Parameters

object SiMap extends LazyLogging with CommunityDetector[SiMapParams] {

  override def prepareGraph(
      graph: TabSwitchActor.TabSwitchGraph
  ): TabSwitchActor.TabSwitchGraph = graph

  override def computeGroups(
      graph: TabSwitchActor.TabSwitchGraph,
      params: SiMapParams
  ): List[Set[TabMeta]] = ???

  override def processGroups(
      tabGroups: List[Set[TabMeta]]
  ): List[Set[TabMeta]] = tabGroups

}
