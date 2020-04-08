package actions

import argonaut._, Argonaut._

case class TabAction(action: String, payload: Json)

object TabAction {
  implicit def TabActionJson =
    casecodec2(TabAction.apply, TabAction.unapply)("action", "payload")
}

case class NewTabAction(url: String)

object NewTabAction {
  implicit def NewTabActionJson =
    casecodec1(NewTabAction.apply, NewTabAction.unapply)("url")
}

case class NotifyAction(message: String)

object NotifyAction {
  implicit def NotifyActionJson =
    casecodec1(NotifyAction.apply, NotifyAction.unapply)("message")
}
