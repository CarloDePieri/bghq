package it.carlodepieri.bghq

import scala.util.Try
import java.time.LocalDateTime
import zio.*
import io.lemonlabs.uri.Url
import net.ruippeixotog.scalascraper.model.Element

import java.time.{Duration => JavaDuration}

abstract class Parser(
    val htmlElement: Element,
    val document: CachedDocument
) {

  extension (s: String) def toSafePrice: Int = (s.toFloat * 100).toInt

  def buildEntry(): Try[GameEntry] =
    for {
      parsedStatus <- status
      parsedPrice <- price
      parsedUrl <- url
      parsedStore <- store
      parsedImage <- image
      parsedTitle <- title
    } yield {
      if (parsedStatus == Status.AVAILABLE || parsedStatus == Status.PREORDER) {
        Entry(
          url = parsedUrl,
          store = parsedStore,
          title = parsedTitle,
          image = parsedImage,
          pageCreatedOn = document.createdAt,
          pageTTL = document.ttl,
          availableStatus = parsedStatus,
          price = parsedPrice,
          discount = discount,
          originalPrice = originalPrice,
          discountEndDate = discountEndDate,
          lang = lang
        )
      } else
        UnavailableEntry(
          url = parsedUrl,
          store = parsedStore,
          title = parsedTitle,
          image = parsedImage,
          pageCreatedOn = document.createdAt,
          pageTTL = document.ttl,
          lang = lang
        )
    }

  val status: Try[Status]
  val price: Try[Option[Int]]
  val url: Try[Url]
  val store: Try[Url]
  val image: Try[Url]
  val title: Try[String]
  val discount: Option[Int]
  val originalPrice: Option[Int]
  val discountEndDate: Option[LocalDateTime]
  val lang: Option[String]
}

trait ElementParser {
  def parse(
      element: Element,
      fromDocument: CachedDocument
  ): Try[GameEntry] = getParser(element, fromDocument).buildEntry()
  def getParser(element: Element, pageDocument: CachedDocument): Parser
}

val a = List()

// Accessor method API
object ElementParser {

  /**
   * Quick access to the [[Parser.buildEntry]] method, which produces a [[GameEntry]].
   *
   * @param element the html element to parse
   * @return a [[Try]] of [[GameEntry]] which allows to handle both successful and failed parse
   */
  def parse(
      element: Element,
      fromDocument: CachedDocument
  ): ZIO[ElementParser, Nothing, Try[GameEntry]] =
    ZIO.serviceWith[ElementParser](_.parse(element, fromDocument))

  /**
   * Get access to the low level [[Parser]] implementation, which can be used to parse only specific field of an element.
   *
   * @param element the html element to parse
   * @return a [[Parser]] implementation
   */
  def getParser(
      element: Element,
      document: CachedDocument
  ): ZIO[ElementParser, Nothing, Parser] =
    ZIO.serviceWith[ElementParser](_.getParser(element, document))
}
