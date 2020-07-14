package persistence

import java.io.File
import java.io.PrintWriter

import scala.util.Try

import com.typesafe.scalalogging.LazyLogging
import io.circe.Json

object Persistence extends LazyLogging {

  val tabsDir =
    s"${System.getProperty("user.home")}${File.separator}tabs${File.separator}"

  def persistJson(fileName: String, jsonData: => Json): Try[Unit] = Try {
    logger.debug(s"> Persisted $fileName")
    persistString(fileName, jsonData.toString())
  }

  def restoreJson(fileName: String): Try[String] = Try {
    val jsonString = scala.io.Source
      .fromFile(new File(tabsDir + fileName))
      .getLines
      .mkString
    logger.debug(s"> Restored $fileName")
    jsonString
  }

  def persistString(fileName: String, content: String): Try[Unit] = Try {
    Some(new PrintWriter(new File(tabsDir + fileName))).foreach { p =>
      p.write(content); p.close
    }
  }

  def restoreString(fileName: String): Try[String] = Try {
    scala.io.Source
      .fromFile(new File(tabsDir + fileName))
      .getLines
      .mkString
  }
}
