package groupnaming

import tabswitches.TabMeta

trait NameGenerator {
  def apply(tabs: Set[TabMeta]): (String, Set[TabMeta])
}
