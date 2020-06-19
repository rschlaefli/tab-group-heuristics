package refactor

import scala.util.Try
import io.circe.Json
import com.typesafe.scalalogging.LazyLogging
import java.io.PrintWriter

object Persistence extends LazyLogging {
  def persistJson(fileName: String, jsonData: => Json): Try[Unit] = Try {
    logger.info(s"> Persisted $fileName")
    persistString(fileName, jsonData.toString())
  }

  def restoreJson(fileName: String): Try[String] = Try {
    val jsonString = scala.io.Source.fromFile(fileName).getLines.mkString
    logger.info(s"> Restored $fileName")
    jsonString
  }

  def persistString(fileName: String, content: String): Try[Unit] = Try {
    Some(new PrintWriter(fileName)).foreach { p => p.write(content); p.close }
  }
}
