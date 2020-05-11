package heuristics

import tabstate.Tab

// a group of tabs
case class TabGroup(
    id: String,
    name: String,
    tabs: List[Tab]
)
