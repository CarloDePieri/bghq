package it.carlodepieri.bghq
package search

import io.lemonlabs.uri.Url
import net.ruippeixotog.scalascraper.model.Document

import java.time.LocalDateTime
import scala.util.Try

trait Search {
  def parseElement(element: Document): Try[GameEntry]
}

trait ElementParser {

  extension (s: String) def toSafePrice: Int = (s.toFloat * 100).toInt

  def getStatus: Status
  def getPrice: Option[Int]
  def getUrl: Url
  def getStore: Url
  def getImage: Url
  def getTitle: String
  def getDiscount: Option[Int]
  def getOriginalPrice: Option[Int]
  def getDiscountEndDate: Option[LocalDateTime]
}
