package it.carlodepieri.bghq
package dungeondice

import scala.util.Try

import zio._

import io.lemonlabs.uri.Url
import net.ruippeixotog.scalascraper.model.{Document, Element}
import net.ruippeixotog.scalascraper.dsl.DSL.*
import net.ruippeixotog.scalascraper.dsl.DSL.Extract.*

class DDPageParser(elementParser: ElementParser) extends PageParser {

  override def nextPage(page: Document): Try[Option[Url]] = Try {
    val nextButton: Element =
      page >> element("li.page-list-item.page-list-next")
    val nextLink: Option[Element] = nextButton >?> element("a")
    nextLink.map {
      a =>
        Url.parse(a.attr("href"))
    }
  }

  override def selectElements(page: Document): Try[List[Element]] =
    Try {
      page >> elementList("#js-product-list > div > article")
    }

  override def parseElement(
                             element: Element,
                             fromDocument: CachedDocument
  ): Try[GameEntry] =
    elementParser.parse(element, fromDocument)

}

object DDPageParser {
  def apply(elementParser: ElementParser): PageParser = new DDPageParser(
    elementParser
  )
  def layer: ZLayer[ElementParser, Nothing, PageParser] = ZLayer {
    for {
      ef <- ZIO.service[ElementParser]
    } yield apply(ef)
  }
}
