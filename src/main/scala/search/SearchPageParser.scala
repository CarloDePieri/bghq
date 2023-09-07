package it.carlodepieri.bghq
package search

import io.lemonlabs.uri.Url
import net.ruippeixotog.scalascraper.model.{Document, Element}

import java.time.LocalDateTime
import scala.util.Try

trait SearchPageParser {
  // TODO a search method that uses streams


  // Parse a page containing search results
  def parseDocument(page: Document): Try[(List[Try[GameEntry]], Option[Url])] =
    for {
      elements <- selectElements(page)
      next <- nextPage(page)
    } yield (
      elements.map(parseElement),
      next
    )

  // Return the next page link, if present
  def nextPage(page: Document): Try[Option[Url]]

  // Return a list of html elements, each containing a game search result
  def selectElements(
      page: Document
  ): Try[List[Element]]

  // Parse a single html element, containing a game search result
  def parseElement(element: Element): Try[GameEntry]
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
