package it.carlodepieri.bghq
package search

import io.lemonlabs.uri.Url
import net.ruippeixotog.scalascraper.model.{Document, Element}
import zio._
import zio.Chunk
import zio.stream.*

import java.time.LocalDateTime
import scala.util.{Failure, Success, Try}

trait SearchPageParser {

  def search(
      query: String,
      skipCache: Boolean = false
  ): ZStream[CachedDocumentService, Throwable, Try[GameEntry]] = {
    // build a 'paginated' stream
    ZStream.paginateChunkZIO(
      // Start from the search url build from the query
      getSearchUrl(query)
    )(
      // This function is a url => (Chunk[Try[GameEntry]] -> Option[String]])
      // After every call, elements from the Chunk will be added to the stream.
      // While the Option is Some(s), this function will be called again with s as the new url
      url =>
        for {
          cds <- ZIO.service[CachedDocumentService]
          doc <- cds.get(url, skipCache)
        } yield {
          parseDocument(doc) match {
            case Success((results, nextPageUrl)) =>
              Chunk.fromIterable(results) -> nextPageUrl.map(_.toString)
            case Failure(e) => throw e
          }
        }
    )
  }

  /**
   * Return a search page Url from a string query.
   */
  def getSearchUrl(query: String): String

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
  def selectElements(
      page: Document
  ): Try[List[Element]]

  /**
   * Try to parse a single html element, containing a game search result.
   */
  def parseElement(element: Element): Try[GameEntry]
}
