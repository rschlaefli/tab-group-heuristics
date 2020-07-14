package statistics

import java.time.Instant
import tabstate.Tab
import statistics.StatisticsActor.SuggestionInteraction

sealed trait StatisticsEvent {
  def timestamp: Long = Instant.now.getEpochSecond()
}

case class TabSwitchEvent(fromTab: Tab, toTab: Tab) extends StatisticsEvent

case class SuggestionInteractionEvent(event: SuggestionInteraction)
    extends StatisticsEvent
