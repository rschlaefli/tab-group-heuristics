package events

import com.typesafe.scalalogging.Logger
import io.circe._, io.circe.parser._

class TabEvent

object TabEvent {
  def decodeEventFromMessage(message: String): Option[TabEvent] = {
    // parse the incoming JSON
    val json: Json = parse(message).getOrElse(Json.Null)

    // get the action type from the json message
    val cursor = json.hcursor
    val action: String = cursor.get[String]("action").getOrElse("NULL")

    // depending on the action type, decode into the appropriate TabEvent
    action match {
      case "CREATE" => {
        cursor.get[TabCreateEvent]("payload") match {
          case Left(decodeError) => None
          case Right(value)      => Some(value)
        }
      }
      case "UPDATE" => {
        cursor.get[TabUpdateEvent]("payload") match {
          case Left(decodeError) => None
          case Right(value)      => Some(value)
        }
      }
      case "ACTIVATE" => {
        cursor.get[TabActivateEvent]("payload") match {
          case Left(decodeError) => None
          case Right(value)      => Some(value)
        }
      }
      case "REMOVE" => {
        cursor.get[TabRemoveEvent]("payload") match {
          case Left(decodeError) => None
          case Right(value)      => Some(value)
        }
      }
    }
  }
}
