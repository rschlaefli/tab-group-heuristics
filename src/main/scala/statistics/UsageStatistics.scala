package statistics

import io.circe._
import io.circe.generic.semiauto._

case class UsageStatistics(
    activeSeconds: Int = 0,
    actionRequested: Boolean = false,
    suggestionInteractions: Int = 0
) {
  def timeIncremented(increment: Int) =
    this.copy(activeSeconds = activeSeconds + increment)
  def logInteractions(count: Int) =
    this.copy(suggestionInteractions = suggestionInteractions + count)
  def logActionRequest() =
    this.copy(actionRequested = true)
}

object UsageStatistics {
  implicit val usageDecoder: Decoder[UsageStatistics] = deriveDecoder
  implicit val usageEncoder: Encoder[UsageStatistics] = deriveEncoder
}
