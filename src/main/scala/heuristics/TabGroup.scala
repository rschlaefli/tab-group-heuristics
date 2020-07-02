package heuristics

import io.circe._
import io.circe.generic.semiauto._
import tabswitches.TabMeta
import java.security.MessageDigest
import java.math.BigInteger

case class TabGroup(
    id: String,
    name: String,
    tabs: Set[TabMeta]
) {
  def asTuple: (String, String, Set[TabMeta]) = {
    (id, name, tabs.toSet)
  }

  def withId(newId: String) = {
    TabGroup(newId, name, tabs)
  }

  def withoutTabs(tabHashes: Set[String]) = {
    TabGroup(id, name, tabs.filter(tab => !tabHashes.contains(tab.hash)))
  }
}

object TabGroup {
  implicit val tabGroupDecoder: Decoder[TabGroup] = deriveDecoder
  implicit val tabGroupEncoder: Encoder[TabGroup] = deriveEncoder

  def apply(tuple: (String, Set[TabMeta])): TabGroup = {
    val groupHash = md5(tuple._2.map(_.hash).toArray.sorted.mkString)
    TabGroup(groupHash, tuple._1, tuple._2.toSet)
  }

  def md5(s: String): String = {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(s.getBytes)
    val bigInt = new BigInteger(1, digest)
    bigInt.toString(16)
  }
}
