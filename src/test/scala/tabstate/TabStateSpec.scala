import org.scalatest._
import tabstate.{Tab, TabState}

class TabStateSpecSpec extends FunSuite with DiagrammedAssertions {
  var tabstate: List[Tab] = null

  test("TabState should initialize the state") {
    tabstate = TabState.initialize
    assert(tabstate.size === 0)
  }

  // test("TabState should append newly created tabs") {
  //   val event = """
  //     {
  //       "action": "CREATE",
  //       "payload": {
  //         "active": false,
  //         "highlighted": false,
  //         "id": 1,
  //         "index": 0,
  //         "lastAccessed": 10000,
  //         "pinned": false,
  //         "title": "Test Tab",
  //         "url": "https://www.test.com",
  //         "windowId": 1
  //       }
  //     }
  //   """.decodeOption[TabEvent].get

  //   tabstate = TabState.processEvent(tabstate, event)

  //   assert(tabstate.size === 1)
  // }
}
