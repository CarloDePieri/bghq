package it.carlodepieri.bghq

import scala.util.{Success, Try}
import java.time.LocalDateTime
import zio.*
import io.lemonlabs.uri.Url
import net.ruippeixotog.scalascraper.model.{Document, Element}
import zio.stream.ZStream

/**
 * Describes ...TODO
 */
trait PageParser {
  def parsePage(page: Document): Try[List[Try[GameEntry]]] = for {
    elements <- selectElements(page)
  } yield elements.map(parseElement)

  def nextPage(page: Document): Try[Option[Url]]

  def selectElements(page: Document): Try[List[Element]]

  def parseElement(element: Element): Try[GameEntry]

  def crawl(
      firstPage: Url,
      skipCache: Boolean = false
  ): ZStream[CachedDocumentService, Throwable, Try[GameEntry]] = {
    // build a 'paginated' stream
    ZStream.paginateChunkZIO(
      // Start from the provided first page Url
      firstPage
    )(
      // This function is a url => (Chunk[Try[GameEntry]] -> Option[String]])
      // After every call, elements from the Chunk will be added to the stream.
      // While the Option is Some(s), this function will be called again with s as the new url
      url =>
        for {
          cds <- ZIO.service[CachedDocumentService]
          doc <- cds.get(url.toString, skipCache)
        } yield (for {
          results <- parsePage(doc)
          nextPageUrl <- nextPage(doc)
        } yield Chunk.fromIterable(results) -> nextPageUrl).get
    )
  }
}

// Accessor method API
object PageParser {

  /**
   * Parse a page containing search results. Try to return a List of Try[GameEntry], which allows to parse together
   * search results and errors.
   */
  def parsePage(
      page: Document
  ): ZIO[PageParser, Nothing, Try[List[Try[GameEntry]]]] =
    ZIO.serviceWith[PageParser](_.parsePage(page))

  /**
   * Try to return the next page link, if present.
   */
  def nextPage(
      page: Document
  ): ZIO[PageParser, Nothing, Try[Option[Url]]] =
    ZIO.serviceWith[PageParser](_.nextPage(page))

  /**
   * Try to return a list of html elements, each containing a game search result.
   */
  def selectElements(
      page: Document
  ): ZIO[PageParser, Nothing, Try[List[Element]]] =
    ZIO.serviceWith[PageParser](_.selectElements(page))

  /**
   * Try to parse a single html element, containing a game search result.
   */
  def parseElement(
      element: Element
  ): ZIO[PageParser, Nothing, Try[GameEntry]] =
    ZIO.serviceWith[PageParser](_.parseElement(element))

  /**
   * Crawl, starting from the `firstPage` [[Url]]. Return a [[ZStream]] where all parsed
   * [[Try]] of [[GameEntry]] will be put.
   *
   * This stream is a `paginated` and `lazy` one:
   *  - after each page is parsed, its resulting [[GameEntry]] will be put into the stream
   *  - only when the first entry of a page would be consumed by the stream, that page will be requested and parsed
   *
   * @param firstPage the [[Url]] of the page from which to start the crawl
   * @param skipCache whether to skip the [[CachedDocumentService]] cache
   * @return a stream with the parsed entries
   */
  def crawl(
      firstPage: Url,
      skipCache: Boolean = false
  ): ZIO[PageParser, Nothing, ZStream[CachedDocumentService, Throwable, Try[
    GameEntry
  ]]] =
    ZIO.serviceWith[PageParser](_.crawl(firstPage, skipCache))
}
