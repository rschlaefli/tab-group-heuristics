package communitydetection

trait CommunityDetectorParameters {

  /**
    * The maximum number of groups to return
    */
  def maxGroups: Int

  /**
    * Remove groups with less nodes
    */
  def minGroupSize: Int

  /**
    * Remove groups with more nodes
    */
  def maxGroupSize: Int
}
