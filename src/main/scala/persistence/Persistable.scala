package persistence

import scala.util.Try
import io.circe.Json
import com.typesafe.scalalogging.LazyLogging
import java.io.PrintWriter

trait Persistable {
  def persist: Try[Unit]
  def restore: Try[Unit]
}

object Persistable extends LazyLogging {
  def persistJson(fileName: String, jsonData: => Json): Try[Unit] = Try {
    logger.info(s"> Writing json to $fileName")
    persistString(fileName, jsonData.toString())
  }

  def restoreJson(fileName: String): Try[String] = Try {
    val jsonString = scala.io.Source.fromFile(fileName).getLines.mkString
    logger.info(s"> Restored JSON from file: $jsonString")
    jsonString
  }

  def persistString(fileName: String, content: String): Try[Unit] = Try {
    Some(new PrintWriter(fileName)).foreach { p => p.write(content); p.close }
  }
}
