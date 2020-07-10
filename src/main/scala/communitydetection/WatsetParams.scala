package communitydetection

import io.circe._
import io.circe.generic.semiauto._

case class WatsetParams(expansion: Int = 2, powerCoefficient: Double = 2)
    extends CommunityDetectorParameters

object WatsetParams {
  implicit val watsetParamsDecoder: Decoder[WatsetParams] =
    deriveDecoder
}
