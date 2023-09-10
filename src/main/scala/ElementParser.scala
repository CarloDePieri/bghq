package it.carlodepieri.bghq

import scala.util.Try
import java.time.LocalDateTime

import zio._

import io.lemonlabs.uri.Url
import net.ruippeixotog.scalascraper.model.Element

abstract class Parser(val htmlElement: Element) {

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
          parsedUrl,
          parsedStore,
          parsedTitle,
          parsedImage,
          parsedStatus,
          parsedPrice,
          discount,
          originalPrice,
          discountEndDate,
          lang
        )
      } else
        UnavailableEntry(
          parsedUrl,
          parsedStore,
          parsedTitle,
          parsedImage,
          lang
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
  def parse(element: Element): Try[GameEntry] = getParser(element).buildEntry()
  def getParser(element: Element): Parser
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
  def parse(element: Element): ZIO[ElementParser, Nothing, Try[GameEntry]] =
    ZIO.serviceWith[ElementParser](_.parse(element))

  /**
   * Get access to the low level [[Parser]] implementation, which can be used to parse only specific field of an element.
   *
   * @param element the html element to parse
   * @return a [[Parser]] implementation
   */
  def getParser(element: Element): ZIO[ElementParser, Nothing, Parser] =
    ZIO.serviceWith[ElementParser](_.getParser(element))
}
