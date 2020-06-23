package communitydetection

import org.scalatest._
import flatspec._
import matchers._
import org.scalactic.source.Position
import tabswitches.TabSwitchMeta
import tabswitches.SwitchMapActor
import tabswitches.GraphUtils
import com.typesafe.scalalogging.LazyLogging
import persistence.Persistence

class CommunityDetectorSpec
    extends AnyFlatSpec
    with should.Matchers
    with BeforeAndAfter
    with LazyLogging {

  import tabswitches.TabSwitchActor.TabSwitchGraph

  var tabSwitchMap: Map[String, TabSwitchMeta] = null
  var tabSwitchGraph: TabSwitchGraph = null

  before {
    SwitchMapActor.restoreTabSwitchMap foreach {
      case Right(restoredMap) => {
        tabSwitchMap = restoredMap.toMap
        tabSwitchGraph = GraphUtils.processSwitchMap(tabSwitchMap)
      }
      case _ =>
    }

  }

  // "Watset" should "produce a valid clustering (2, 2)" in {
  //   val tabGroups = Watset(tabSwitchGraph, WatsetParams(2, 2))
  //   assert(tabGroups.size > 0)

  //   Persistence.persistString(
  //     "clusters_markov_2-2.txt",
  //     tabGroups.map(_.toString()).mkString("\n")
  //   )
  // }

  // "Watset" should "produce a valid clustering (1, 2)" in {
  //   val tabGroups = Watset(tabSwitchGraph, WatsetParams(1, 2))
  //   assert(tabGroups.size > 0)

  //   Persistence.persistString(
  //     "clusters_markov_1-2.txt",
  //     tabGroups.map(_.toString()).mkString("\n")
  //   )
  // }

  // "Watset" should "produce a valid clustering (3, 2)" in {
  //   val tabGroups = Watset(tabSwitchGraph, WatsetParams(3, 2))
  //   assert(tabGroups.size > 0)

  //   Persistence.persistString(
  //     "clusters_markov_3-2.txt",
  //     tabGroups.map(_.toString()).mkString("\n")
  //   )
  // }

  // "Watset" should "produce a valid clustering (4, 2)" in {
  //   val tabGroups = Watset(tabSwitchGraph, WatsetParams(4, 2))
  //   assert(tabGroups.size > 0)

  //   Persistence.persistString(
  //     "clusters_markov_4-2.txt",
  //     tabGroups.map(_.toString()).mkString("\n")
  //   )
  // }

  // "Watset" should "produce a valid clustering (9, 2)" in {
  //   val tabGroups = Watset(tabSwitchGraph, WatsetParams(9, 2))
  //   assert(tabGroups.size > 0)

  //   Persistence.persistString(
  //     "clusters_markov_9-2.txt",
  //     tabGroups.map(_.toString()).mkString("\n")
  //   )
  // }

  // "Watset" should "produce a valid clustering (9, 5)" in {
  //   val tabGroups = Watset(tabSwitchGraph, WatsetParams(9, 5))
  //   assert(tabGroups.size > 0)

  //   Persistence.persistString(
  //     "clusters_markov_9-5.txt",
  //     tabGroups.map(_.toString()).mkString("\n")
  //   )
  // }

  // "Watset" should "produce a valid clustering (9, 1)" in {
  //   val tabGroups = Watset(tabSwitchGraph, WatsetParams(9, 1))
  //   assert(tabGroups.size > 0)

  //   Persistence.persistString(
  //     "clusters_markov_9-1.txt",
  //     tabGroups.map(_.toString()).mkString("\n")
  //   )
  // }

  "SiMap" should "produce a valid clustering" in {
    val tabGroups = SiMap(tabSwitchGraph, SiMapParams())
    assert(tabGroups.size > 0)

    Persistence.persistString(
      "clusters_simap.txt",
      tabGroups.map(_.toString()).mkString("\n")
    )
  }

}
