package communitydetection

import io.circe._
import io.circe.generic.semiauto._
import network.optimization.CPMapParameters

case class SiMapParams(
    /**
      * Probability of choosing to teleport instead of following the transition probability matrix
      * to guarantee convergence of G * p = p
      */
    tau: Float = 0.15.toFloat,
    /**
      * Start resolution to search for the best resolution
      */
    resStart: Float = 0.0001.toFloat,
    /**
      * End resolution
      */
    resEnd: Float = 0.05.toFloat,
    /**
      * Accuracy of the best solution, e.g. when accuracy is 0.1,
      * the solution is refined util this close to the best resolution found so far
      */
    resAcc: Float = 0.001.toFloat,
    /**
      * Process only the largest connected component
      */
    largestCC: Boolean = false
) extends CommunityDetectorParameters {

  def asCPMapParameters =
    new CPMapParameters(tau, false, false, 1, resStart, resEnd, resAcc)

}

object SiMapParams {
  implicit val siMapParamsDecoder: Decoder[SiMapParams] =
    deriveDecoder
}
