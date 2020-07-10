package communitydetection

trait CommunityDetectorParameters {

  /**
    * The maximum number of groups to return
    */
  def maxGroups: Int = 10

  /**
    * Remove groups with less nodes
    */
  def minGroupSize: Int = 3

  /**
    * Remove groups with more nodes
    */
  def maxGroupSize: Int = 10

}
