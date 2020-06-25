package persistence

import java.io.PrintWriter

import scala.util.Try

import com.typesafe.scalalogging.LazyLogging
import io.circe.Json

object Persistence extends LazyLogging {
  def persistJson(fileName: String, jsonData: => Json): Try[Unit] = Try {
    logger.debug(s"> Persisted $fileName")
    persistString(fileName, jsonData.toString())
  }

  def restoreJson(fileName: String): Try[String] = Try {
    val jsonString = scala.io.Source.fromFile(fileName).getLines.mkString
    logger.debug(s"> Restored $fileName")
    jsonString
  }

  def persistString(fileName: String, content: String): Try[Unit] = Try {
    Some(new PrintWriter(fileName)).foreach { p => p.write(content); p.close }
  }
}
