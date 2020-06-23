package tabswitches

import org.jgrapht.Graph
import org.jgrapht.graph.DefaultWeightedEdge

trait CommunityDetector {

  type TabSwitchGraph = Graph[TabMeta, DefaultWeightedEdge]

  def apply(graph: TabSwitchGraph): List[Set[TabMeta]] = {

    if (graph == null) return List()

    val preparedGraph = prepareGraph(graph)

    val isGraphTooSmall = preparedGraph
      .vertexSet()
      .size() < 2
    val isGraphUnconnected = preparedGraph.edgeSet().size() == 0

    if (isGraphTooSmall || isGraphUnconnected) return List()

    val tabGroups = computeGroups(preparedGraph)

    processGroups(tabGroups)

  }

  /**
    * Prepare the tab switch graph for analysis with the algorithm
    *
    * @param graph The raw tab switch graph
    * @return The pre-processed tab switch graph
    */
  def prepareGraph(graph: TabSwitchGraph): TabSwitchGraph

  /**
    * Apply the community detection algorithm to the tab switch graph
    *
    * @param graph The pre-processed tab switch graph
    * @return The generated list of tab groups
    */
  def computeGroups(graph: TabSwitchGraph): List[Set[TabMeta]]

  /**
    * Perform postprocessing on the generated tab groups
    *
    * @param tabGroups The generated list of tab groups
    * @return The post-processed list of tab groups
    */
  def processGroups(tabGroups: List[Set[TabMeta]]): List[Set[TabMeta]]

}
