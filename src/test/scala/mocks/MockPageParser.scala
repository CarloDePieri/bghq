package it.carlodepieri.bghq
package mocks

import scala.util.Try
import zio.*
import zio.mock.*
import io.lemonlabs.uri.Url
import net.ruippeixotog.scalascraper.model.{Document, Element}
import zio.stream.ZStream

object MockPageParser extends Mock[PageParser] {
  object ParsePage extends Method[Document, Nothing, Try[List[Try[GameEntry]]]]
  object NextPage extends Method[Document, Nothing, Try[Option[Url]]]
  object SelectElements extends Method[Document, Nothing, Try[List[Element]]]
  object ParseElement extends Method[Element, Nothing, Try[GameEntry]]
  object Crawl extends Stream[(Url, Boolean), Throwable, Try[GameEntry]]

  override val compose: URLayer[Proxy, PageParser] = {
    ZLayer {
      for {
        proxy <- ZIO.service[Proxy]
      } yield {
        val unsafe = zio.Runtime.default.unsafe
        new PageParser:
          override def parsePage(page: Document): Try[List[Try[GameEntry]]] =
            Unsafe.unsafely {
              unsafe.run(proxy(ParsePage, page)).getOrThrow()
            }
          override def selectElements(page: Document): Try[List[Element]] =
            Unsafe.unsafely {
              unsafe.run(proxy(SelectElements, page)).getOrThrow()
            }
          override def parseElement(element: Element): Try[GameEntry] =
            Unsafe.unsafely {
              unsafe.run(proxy(ParseElement, element)).getOrThrow()
            }
          override def nextPage(page: Document): Try[Option[Url]] =
            Unsafe.unsafely {
              unsafe.run(proxy(NextPage, page)).getOrThrow()
            }
          override def crawl(
              firstPage: Url,
              skipCache: Boolean = false
          ): ZStream[CachedDocumentService, Throwable, Try[GameEntry]] =
            Unsafe.unsafely {
              unsafe
                .run(proxy(Crawl, firstPage, skipCache))
                .getOrThrow()
            }
      }
    }
  }
}
