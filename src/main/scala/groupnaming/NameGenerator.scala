package groupnaming

import tabswitches.TabMeta

trait Parameters

trait NameGenerator[T] {
  def apply(tabs: Set[TabMeta], params: T): (String, Set[TabMeta])
}
