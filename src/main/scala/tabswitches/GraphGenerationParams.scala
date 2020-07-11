package tabswitches

import io.circe._
import io.circe.generic.semiauto._

case class GraphGenerationParams(
    /**
      * Ignore edges with a lower weight
      */
    minWeight: Double = 2,
    /**
      * Forgetting factor
      */
    expireAfter: Int = 14,
    /**
      * Factor to punish switches on the same origin
      */
    sameOriginFactor: Double = 0.3,
    /**
      * Factor to punish similar URLs
      */
    urlSimilarityFactor: Double = 0.5
)

object GraphGenerationParams {
  implicit val graphGenerationParamsDecoder: Decoder[GraphGenerationParams] =
    deriveDecoder
}
