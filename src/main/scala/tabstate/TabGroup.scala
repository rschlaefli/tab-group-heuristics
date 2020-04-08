package tabstate

// a group of tabs
case class TabGroup(
    id: String,
    name: String,
    tabs: List[Tab]
)
