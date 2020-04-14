package persistence

import com.typesafe.scalalogging.LazyLogging
import io.circe.Json
import java.io.PrintWriter
import scala.util.Try
import tabstate.TabState
import io.circe._, io.circe.parser._, io.circe.generic.semiauto._,
io.circe.syntax._
import scala.collection.mutable.{Map, Queue}

object PersistenceEngine extends LazyLogging {

  def apply(): Thread = {
    val persistenceThread = new Thread(() => {
      logger.info("> Starting to persist state")
      while (true) {
        persistJson("tab_switches_base.json", TabState.tabBaseSwitches.asJson)
        persistJson("tab_hashes_base.json", TabState.tabBaseHashes.asJson)
        persistJson(
          "tab_switches_origin.json",
          TabState.tabOriginSwitches.asJson
        )
        persistJson("tab_hashes_origin.json", TabState.tabOriginHashes.asJson)
        Thread.sleep(60000)
      }
    })
    persistenceThread.setName("Persistence")
    persistenceThread.setDaemon(true)
    persistenceThread
  }

  def restoreInitialState() = {
    restoreJson("tab_switches_base.json")
      .map(decode[Map[String, Map[String, Int]]])
      .foreach {
        case Left(value)        =>
        case Right(restoredMap) => TabState.tabBaseSwitches = restoredMap
      }

    restoreJson("tab_hashes_base.json")
      .map(decode[Map[String, String]])
      .foreach {
        case Left(value)        =>
        case Right(restoredMap) => TabState.tabBaseHashes = restoredMap
      }

    restoreJson("tab_switches_origin.json")
      .map(decode[Map[String, Map[String, Int]]])
      .foreach {
        case Left(value)        =>
        case Right(restoredMap) => TabState.tabOriginSwitches = restoredMap
      }

    restoreJson("tab_hashes_origin.json")
      .map(decode[Map[String, String]])
      .foreach {
        case Left(value)        =>
        case Right(restoredMap) => TabState.tabOriginHashes = restoredMap
      }
  }

  def persistJson(fileName: String, jsonData: => Json): Try[Unit] = Try {
    logger.info(s"> Writing json to $fileName")
    Some(new PrintWriter(fileName)).foreach { p =>
      p.write(jsonData.toString()); p.close
    }
  }

  def restoreJson(fileName: String): Try[String] = Try {
    val jsonString = scala.io.Source.fromFile(fileName).getLines.mkString
    logger.info(s"> Restored JSON from file: $jsonString")
    jsonString
  }

}
