package communitydetection

import tabswitches.TabMeta

trait Parameters

trait CommunityDetector[S, T] {

  import tabswitches.TabSwitchActor.TabSwitchGraph

  def apply(graph: TabSwitchGraph, params: T): List[Set[TabMeta]] = {

    if (graph == null) return List()

    val preparedGraph = prepareGraph(graph)

    val tabGroups = computeGroups(preparedGraph, params)

    processGroups(tabGroups)

  }

  /**
    * Prepare the tab switch graph for analysis with the algorithm
    *
    * @param graph The raw tab switch graph
    * @return The pre-processed tab switch graph
    */
  def prepareGraph(graph: TabSwitchGraph): S

  /**
    * Apply the community detection algorithm to the tab switch graph
    *
    * @param graph The pre-processed tab switch graph
    * @return The generated list of tab groups
    */
  def computeGroups(
      graph: S,
      params: T
  ): List[Set[TabMeta]]

  /**
    * Perform postprocessing on the generated tab groups
    *
    * @param tabGroups The generated list of tab groups
    * @return The post-processed list of tab groups
    */
  def processGroups(tabGroups: List[Set[TabMeta]]): List[Set[TabMeta]]

}
