package heuristics

import io.circe._
import io.circe.generic.semiauto._

case class HeuristicsParameters(
    /**
      * Which algorithm to use for clique detection
      */
    algorithm: String = "all",
    /**
      * The minimum overlap between groups to classify the suggestion as an extension
      */
    minOverlap: Double = 0.2d
)

object HeuristicsParameters {
  implicit val heuristicsParametersDecoder: Decoder[HeuristicsParameters] =
    deriveDecoder
}
