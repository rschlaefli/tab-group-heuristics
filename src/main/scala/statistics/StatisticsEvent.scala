package statistics

import java.time.Instant

import statistics.StatisticsActor.SuggestionInteraction
import tabstate.Tab

sealed trait StatisticsEvent {
  def timestamp: Long = Instant.now.getEpochSecond()
}

case class TabSwitchEvent(fromTab: Tab, toTab: Tab, switchTime: Int)
    extends StatisticsEvent

case class SuggestionInteractionEvent(event: SuggestionInteraction)
    extends StatisticsEvent

case class CuratedGroupOpenEvent(focusMode: Boolean) extends StatisticsEvent
case class CuratedGroupCloseEvent() extends StatisticsEvent
