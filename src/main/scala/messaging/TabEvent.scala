package messaging

import io.circe._, io.circe.parser._, io.circe.generic.semiauto._
import com.typesafe.scalalogging.LazyLogging

import util._
import tabstate.Tab
import heuristics.TabGroup
import main.Main

sealed class TabEvent

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
    lastAccessed: Option[Double],
    openerTabId: Option[Int],
    sessionId: Option[Int],
    successorTabId: Option[Int]
) extends TabEvent

object TabUpdateEvent {
  implicit val tabUpdateEventDecoder: Decoder[TabUpdateEvent] =
    deriveDecoder
}

object TabEvent extends LazyLogging {

  def decodeEventFromMessage(message: String): Option[TabEvent] = {
    // parse the incoming JSON
    val json: Json = parse(message).getOrElse(Json.Null)
    // logger.debug(s"> Parsed JSON from message => ${json.toString()}")

    // get the action type from the json message
    val cursor = json.hcursor
    val action: String = cursor.get[String]("action").getOrElse("NULL")

    // depending on the action type, decode into the appropriate TabEvent
    val tabEvent = action match {
      case "CREATE" | "UPDATE" => {
        Utils.extractDecoderResult(cursor.get[TabUpdateEvent]("payload"))
      }

      case "ACTIVATE" => {
        Utils.extractDecoderResult(cursor.get[TabActivateEvent]("payload"))
      }

      case "REMOVE" => {
        Utils.extractDecoderResult(cursor.get[TabRemoveEvent]("payload"))
      }

      case "INIT_TABS" => {
        Utils.extractDecoderResult(
          cursor.get[TabInitializationEvent]("payload")
        )
      }

      case "INIT_GROUPS" => {
        Utils.extractDecoderResult(
          cursor.get[TabGroupUpdateEvent]("payload")
        )
      }

      case "PAUSE" => {
        logger.warn("> PAUSING")
        None
      }

      case "RESUME" => {
        logger.warn("> RESUMING")
        None
      }

      case _ => {
        logger.warn(s"> Unknown tab event received: $action")
        None
      }

      // TODO: case MOVE
      // TODO: case ATTACH
      // TODO: case GROUP_ASSOC
      // TODO: case GROUP_REMOVE
    }

    tabEvent
  }
}
