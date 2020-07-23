package heuristics

import java.math.BigInteger
import java.security.MessageDigest

import io.circe._
import io.circe.generic.semiauto._
import tabswitches.TabMeta

case class TabGroup(
    id: String,
    name: String,
    tabs: Set[TabMeta]
) {
  def asTuple: (String, String, Set[TabMeta]) = {
    (id, name, tabs.toSet)
  }

  def withId(newId: String) = {
    this.copy(id = newId)
  }

  def withoutTabs(tabHashes: Set[String]) = {
    this.copy(tabs = tabs.filter(tab => !tabHashes.contains(tab.hash)))
  }

  def ranked = {
    this.copy(tabs = tabs.toList.sortBy(_.pageRank).toSet)
  }
}

object TabGroup {
  implicit val tabGroupDecoder: Decoder[TabGroup] = deriveDecoder
  implicit val tabGroupEncoder: Encoder[TabGroup] = deriveEncoder

  def apply(tuple: (String, Set[TabMeta])): TabGroup = {
    val groupHash = md5(tuple._2.map(_.hash).toArray.sorted.mkString)
    TabGroup(groupHash, tuple._1, tuple._2.toSet).ranked
  }

  def md5(s: String): String = {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(s.getBytes)
    val bigInt = new BigInteger(1, digest)
    bigInt.toString(16)
  }
}
