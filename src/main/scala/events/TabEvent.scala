package events

import com.typesafe.scalalogging.Logger
import io.circe._, io.circe.parser._
import com.typesafe.scalalogging.LazyLogging

class TabEvent

object TabEvent extends LazyLogging {
  def decodeEventFromMessage(message: String): Option[TabEvent] = {
    // parse the incoming JSON
    val json: Json = parse(message).getOrElse(Json.Null)
    logger.debug(json.toString())

    // get the action type from the json message
    val cursor = json.hcursor
    val action: String = cursor.get[String]("action").getOrElse("NULL")
    logger.debug(action)

    // depending on the action type, decode into the appropriate TabEvent
    val tabEvent = action match {
      case "CREATE" => {
        cursor.get[TabUpdateEvent]("payload") match {
          case Left(decodeError) => {
            logger.error(decodeError.message)
            None
          }
          case Right(value) => Some(value)
        }
      }
      case "UPDATE" => {
        cursor.get[TabUpdateEvent]("payload") match {
          case Left(decodeError) => {
            logger.error(decodeError.message)
            None
          }
          case Right(value) => Some(value)
        }
      }
      case "ACTIVATE" => {
        cursor.get[TabActivateEvent]("payload") match {
          case Left(decodeError) => {
            logger.error(decodeError.message)
            None
          }
          case Right(value) => Some(value)
        }
      }
      case "REMOVE" => {
        cursor.get[TabRemoveEvent]("payload") match {
          case Left(decodeError) => {
            logger.error(decodeError.message)
            None
          }
          case Right(value) => Some(value)
        }
      }
      // TODO: case MOVE
      // TODO: case ATTACH
      // TODO: case GROUP_ASSOC
      // TODO: case GROUP_REMOVE
    }

    logger.debug(tabEvent.toString())

    tabEvent
  }
}
