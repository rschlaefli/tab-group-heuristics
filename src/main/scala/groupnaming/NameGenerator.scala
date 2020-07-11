package groupnaming

import tabswitches.TabMeta

trait NameGeneratorParameters

trait NameGenerator[T <: NameGeneratorParameters] {
  def apply(tabs: Set[TabMeta], params: T): (String, Set[TabMeta])
}
