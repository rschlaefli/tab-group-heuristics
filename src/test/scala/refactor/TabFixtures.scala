package refactor

import tabstate.Tab

object TabFixtures {
  val Tab1 = Tab(
    id = 0,
    index = 0,
    pinned = false,
    title = "Test 1",
    normalizedTitle = "Test 1",
    url = "http://test1.com/something",
    baseUrl = "http://test1.com",
    hash = "1234",
    origin = "test1.com",
    windowId = 0,
    lastAccessed = None,
    openerTabId = None,
    sessionId = None,
    successorTabId = None
  )

  val Tab2 = Tab(
    id = 1,
    index = 1,
    pinned = false,
    title = "Test 2",
    normalizedTitle = "Test 2",
    url = "http://test2.com/something",
    baseUrl = "http://test2.com",
    hash = "5678",
    origin = "test2.com",
    windowId = 0,
    lastAccessed = None,
    openerTabId = None,
    sessionId = None,
    successorTabId = None
  )

  val Tab2Updated = Tab(
    id = 1,
    index = 1,
    pinned = false,
    title = "Test 2 Updated",
    normalizedTitle = "Test 2 Updated",
    url = "http://test2.com/other",
    baseUrl = "http://test2.com",
    hash = "6789",
    origin = "test2.com",
    windowId = 0,
    lastAccessed = None,
    openerTabId = None,
    sessionId = None,
    successorTabId = None
  )

  val Tab3 = Tab(
    id = 2,
    index = 2,
    pinned = false,
    title = "Test 3",
    normalizedTitle = "Test 3",
    url = "http://test3.com/something",
    baseUrl = "http://test3.com",
    hash = "9876",
    origin = "test3.com",
    windowId = 0,
    lastAccessed = None,
    openerTabId = None,
    sessionId = None,
    successorTabId = None
  )
}
