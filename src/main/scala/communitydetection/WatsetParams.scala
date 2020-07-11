package communitydetection

import io.circe._
import io.circe.generic.semiauto._

case class WatsetParams(
    expansion: Int = 2,
    powerCoefficient: Double = 2,
    /**
      * The maximum number of groups to return
      */
    maxGroups: Int = 10,
    /**
      * Remove groups with less nodes
      */
    minGroupSize: Int = 3,
    /**
      * Remove groups with more nodes
      */
    maxGroupSize: Int = 10
) extends CommunityDetectorParameters

object WatsetParams {
  implicit val watsetParamsDecoder: Decoder[WatsetParams] =
    deriveDecoder
}
