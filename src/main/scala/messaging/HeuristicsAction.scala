package messaging

import io.circe._, io.circe.parser._, io.circe.generic.semiauto._

case class HeuristicsAction(action: String, payload: Json)

object HeuristicsAction {
  implicit val heuristicsActionEncoder: Encoder[HeuristicsAction] =
    deriveEncoder
}
