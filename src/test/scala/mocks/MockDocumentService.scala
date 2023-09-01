package it.carlodepieri.bghq
package mocks

import zio._
import zio.mock.*

import net.ruippeixotog.scalascraper.model.Document

object MockDocumentService extends Mock[DocumentService] {

  object Get extends Effect[String, Throwable, Document]
  object ParseString extends Effect[String, Throwable, Document]

  override val compose: URLayer[Proxy, DocumentService] =
    ZLayer {
      for {
        proxy <- ZIO.service[Proxy]
      } yield new DocumentService {
        override def get(url: String): Task[Document] =
          proxy(Get, url)
        override def parseString(html: String): Task[Document] =
          proxy(ParseString, html)
      }
    }
}
