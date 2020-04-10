package events

import org.scalatest._

class TabEventSpec extends FunSuite with DiagrammedAssertions {

  test("TabEvent should correctly process a create event") {
    val message = """
      {
        "action": "CREATE",
        "payload": {
          "active": false,
          "highlighted": false,
          "id": 1,
          "index": 0,
          "lastAccessed": 10000,
          "pinned": false,
          "title": "Test Tab",
          "url": "https://www.test.com",
          "windowId": 1
        }
      }
    """

    val event: TabEvent = TabEvent.decodeEventFromMessage(message).get

    println(event)
  }

  test("TabEvent should correctly process an update event") {
    val message = """
      {
        "action": "UPDATE",
        "payload": {
          "active": true,
          "highlighted": false,
          "id": 1,
          "index": 0,
          "lastAccessed": 10000,
          "pinned": false,
          "title": "Test Tab 2",
          "url": "https://www.test.com",
          "windowId": 1
        }
      }
    """

    val event: TabEvent = TabEvent.decodeEventFromMessage(message).get

    println(event)
  }

  test("TabEvent should correctly process an activate event") {
    val message = """
      {
        "action": "ACTIVATE",
        "payload": {
          "id": 1,
          "windowId": 1
        }
      }
    """

    val event: TabEvent = TabEvent.decodeEventFromMessage(message).get

    println(event)
  }

  test("TabEvent should correctly process a remove event") {
    val message = """
      {
        "action": "REMOVE",
        "payload": {
          "id": 1,
          "windowId": 1
        }
      }
    """

    val event: TabEvent = TabEvent.decodeEventFromMessage(message).get

    println(event)
  }
}
