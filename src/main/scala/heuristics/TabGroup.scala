package heuristics

import io.circe._
import io.circe.generic.semiauto._
import tabswitches.TabMeta
import java.security.MessageDigest
import java.math.BigInteger

case class TabGroup(
    id: String,
    name: String,
    tabs: List[TabMeta]
) {
  def asTuple: (String, Set[TabMeta]) = {
    (name, tabs.toSet)
  }
}

object TabGroup {
  implicit val tabGroupDecoder: Decoder[TabGroup] = deriveDecoder
  implicit val tabGroupEncoder: Encoder[TabGroup] = deriveEncoder

  def apply(tuple: (String, Set[TabMeta])): TabGroup = {
    val tabHashes = tuple._2.map(_.hash).toArray.sorted.mkString
    TabGroup(md5(tabHashes), tuple._1, tuple._2.toList)
  }

  def md5(s: String): String = {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(s.getBytes)
    val bigInt = new BigInteger(1, digest)
    bigInt.toString(16)
  }
}
