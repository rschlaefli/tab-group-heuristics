package messaging

import io.circe._, io.circe.parser._, io.circe.generic.semiauto._

import tabstate.Tab

case class HeuristicsAction(action: String, payload: Json)

object HeuristicsAction {
  implicit val encoder: Encoder[HeuristicsAction] = deriveEncoder

  def actionOnly(action: String) = HeuristicsAction(action, Json.Null)

  def QUERY_TABS = HeuristicsAction("QUERY_TABS", Json.Null)
  def QUERY_GROUPS = HeuristicsAction("QUERY_GROUPS", Json.Null)
  def UPDATE_GROUPS(json: Json) = HeuristicsAction("UPDATE_GROUPS", json)
}

// case class QueryTabsAction() extends HeuristicsAction("QUERY_TABS", Json.Null)

// object QueryTabsAction {
//   implicit val encoder: Encoder[QueryTabsAction] = deriveEncoder
// }

// case class GroupUpdateAction(payload: Json)
//     extends HeuristicsAction("UPDATE_GROUPS", payload)

// object GroupUpdateAction {
//   implicit val encoder: Encoder[GroupUpdateAction] = deriveEncoder
// }
