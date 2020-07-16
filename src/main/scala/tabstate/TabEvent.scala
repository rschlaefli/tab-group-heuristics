package tabstate

import com.typesafe.scalalogging.LazyLogging
import heuristics.HeuristicsParameters
import heuristics.TabGroup
import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser._
import tabstate.Tab
import tabswitches.GraphGenerationParams

sealed class TabEvent

case object PauseEvent extends TabEvent
case object ResumeEvent extends TabEvent

case class RefreshGroupsEvent(
    heuristics: HeuristicsParameters,
    graphGeneration: GraphGenerationParams,
    grouping: Json
) extends TabEvent
object RefreshGroupsEvent {
  implicit val refreshGroupsEventDecoder: Decoder[RefreshGroupsEvent] =
    deriveDecoder
}

case class CuratedGroupCloseEvent() extends TabEvent
object CuratedGroupCloseEvent {
  implicit val curatedGroupCloseEventDecoder: Decoder[CuratedGroupCloseEvent] =
    deriveDecoder
}

case class CuratedGroupOpenEvent(focusMode: Boolean) extends TabEvent
object CuratedGroupOpenEvent {
  implicit val curatedGroupOpenEventDecoder: Decoder[CuratedGroupOpenEvent] =
    deriveDecoder
}

case class SuggestedGroupAcceptEvent(groupHash: String) extends TabEvent
object SuggestedGroupAcceptEvent {
  implicit val suggestedGroupAcceptEventDecoder
      : Decoder[SuggestedGroupAcceptEvent] =
    deriveDecoder
}

case class SuggestedTabAcceptEvent(
    groupHash: String,
    tabHash: String,
    targetGroup: String
) extends TabEvent
object SuggestedTabAcceptEvent {
  implicit val suggestedTabAcceptEventDecoder
      : Decoder[SuggestedTabAcceptEvent] =
    deriveDecoder
}

case class SuggestedGroupDiscardEvent(
    groupHash: String,
    reason: Option[String],
    rating: Option[Int]
) extends TabEvent
object SuggestedGroupDiscardEvent {
  implicit val suggestedGroupDiscardEventDecoder
      : Decoder[SuggestedGroupDiscardEvent] =
    deriveDecoder
}

case class SuggestedTabDiscardEvent(
    groupHash: String,
    tabHash: String,
    reason: Option[String]
) extends TabEvent
object SuggestedTabDiscardEvent {
  implicit val suggestedTabDiscardEventDecoder
      : Decoder[SuggestedTabDiscardEvent] =
    deriveDecoder
}

case class TabInitializationEvent(
    currentTabs: List[Tab]
) extends TabEvent

object TabInitializationEvent {
  implicit val tabInitializationEventDecoder: Decoder[TabInitializationEvent] =
    deriveDecoder
}

case class TabGroupUpdateEvent(
    tabGroups: List[TabGroup]
) extends TabEvent

object TabGroupUpdateEvent {
  implicit val tabGroupUpdateEventDecoder: Decoder[TabGroupUpdateEvent] =
    deriveDecoder
}

case class TabActivateEvent(
    id: Int,
    windowId: Int,
    previousTabId: Option[Int]
) extends TabEvent

object TabActivateEvent {
  implicit val tabActivateEventDecoder: Decoder[TabActivateEvent] =
    deriveDecoder
}

case class TabRemoveEvent(
    id: Int,
    windowId: Int
) extends TabEvent

object TabRemoveEvent {
  implicit val tabRemoveEventDecoder: Decoder[TabRemoveEvent] =
    deriveDecoder
}

case class TabUpdateEvent(
    id: Int,
    index: Int,
    windowId: Int,
    url: String,
    hash: String,
    baseUrl: String,
    origin: String,
    title: String,
    normalizedTitle: String,
    pinned: Boolean,
    lastAccessed: Option[Long],
    openerTabId: Option[Int],
    sessionId: Option[Int],
    successorTabId: Option[Int]
) extends TabEvent

object TabUpdateEvent {
  implicit val tabUpdateEventDecoder: Decoder[TabUpdateEvent] =
    deriveDecoder
}

object TabEvent extends LazyLogging {

  def extractDecoderResult[T](
      decoderResult: Decoder.Result[T]
  ): Option[T] = decoderResult match {
    case Left(decodeError) => {
      logger.error(decodeError.message + decoderResult.toString())
      None
    }
    case Right(value) => Some(value)
  }

  def parseJsonString(message: String): Json = {
    var json: Json = parse(message).getOrElse(Json.Null)

    if (json == Json.Null) {
      val fixedMessage = "{ \"ac" + message
      json = parse(fixedMessage).getOrElse(Json.Null)
      logger.debug(
        s"Fixed payload for message ${if (fixedMessage.length() > 50) fixedMessage.substring(0, 50)
        else fixedMessage}"
      )
    }

    json
  }

  def decodeEventFromMessage(message: String): Option[TabEvent] = {
    val json = parseJsonString(message)

    // get the action type from the json message
    val cursor = json.hcursor
    val action: String = cursor.get[String]("action").getOrElse("NULL")

    // depending on the action type, decode into the appropriate TabEvent
    action match {
      case "CREATE" | "UPDATE" =>
        extractDecoderResult(cursor.get[TabUpdateEvent]("payload"))

      case "ACTIVATE" =>
        extractDecoderResult(cursor.get[TabActivateEvent]("payload"))

      case "REMOVE" =>
        extractDecoderResult(cursor.get[TabRemoveEvent]("payload"))

      case "INIT_TABS" =>
        extractDecoderResult(
          cursor.get[TabInitializationEvent]("payload")
        )

      case "INIT_GROUPS" =>
        extractDecoderResult(
          cursor.get[TabGroupUpdateEvent]("payload")
        )

      case "PAUSE" => Some(PauseEvent)

      case "RESUME" => Some(ResumeEvent)

      case "REFRESH_GROUPS" =>
        extractDecoderResult(
          cursor.get[RefreshGroupsEvent]("payload")
        )

      case "DISCARD_GROUP" =>
        extractDecoderResult(
          cursor.get[SuggestedGroupDiscardEvent]("payload")
        )

      case "DISCARD_TAB" =>
        extractDecoderResult(
          cursor.get[SuggestedTabDiscardEvent]("payload")
        )

      case "ACCEPT_GROUP" =>
        extractDecoderResult(
          cursor.get[SuggestedGroupAcceptEvent]("payload")
        )

      case "ACCEPT_TAB" =>
        extractDecoderResult(
          cursor.get[SuggestedTabAcceptEvent]("payload")
        )

      case "OPEN_GROUP" =>
        extractDecoderResult(
          cursor.get[CuratedGroupOpenEvent]("payload")
        )

      case "CLOSE_GROUP" =>
        extractDecoderResult(
          cursor.get[CuratedGroupCloseEvent]("payload")
        )

      case _ => {
        logger.warn(s"> Unknown tab event received: $action ($message)")
        None
      }

    }

  }
}
