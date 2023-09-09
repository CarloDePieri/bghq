package it.carlodepieri.bghq
package search

import scala.util.Try
import java.time.LocalDateTime

import io.lemonlabs.uri.Url
import net.ruippeixotog.scalascraper.model.{Document, Element}

/**
 * Describes ...TODO
 */
trait PageParser {

  /**
   * Parse a page containing search results. Try to return a List of Try[GameEntry], which allows to parse together
   * search results and errors. Return also an Option[Url] for the next results page.
   */
  def parseDocument(page: Document): Try[(List[Try[GameEntry]], Option[Url])] =
    for {
      elements <- selectElements(page)
      next <- nextPage(page)
    } yield (
      elements.map(parseElement),
      next
    )

  /**
   * Try to return the next page link, if present.
   */
  def nextPage(page: Document): Try[Option[Url]]

  /**
   * Try to return a list of html elements, each containing a game search result.
   */
  def selectElements(page: Document): Try[List[Element]]

  /**
   * Try to parse a single html element, containing a game search result.
   */
  def parseElement(element: Element): Try[GameEntry]
}

//trait ElementParser {
//
//  extension (s: String) def toSafePrice: Int = (s.toFloat * 100).toInt
//
//  def getStatus: Status
//  def getPrice: Option[Int]
//  def getUrl: Url
//  def getStore: Url
//  def getImage: Url
//  def getTitle: String
//  def getDiscount: Option[Int]
//  def getOriginalPrice: Option[Int]
//  def getDiscountEndDate: Option[LocalDateTime]
//}
