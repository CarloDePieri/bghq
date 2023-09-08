package it.carlodepieri.bghq
package mocks

import net.ruippeixotog.scalascraper.model.Document
import zio.*
import zio.mock.*

object MockCachedDocumentService extends Mock[CachedDocumentService] {

  object Get extends Effect[(String, Boolean), Throwable, Document]

  override val compose: URLayer[Proxy, CachedDocumentService] =
    ZLayer {
      for {
        proxy <- ZIO.service[Proxy]
      } yield new CachedDocumentService {
        override def get(
            url: String,
            skipCache: Boolean = false
        ): Task[Document] =
          proxy(Get, url, skipCache)
      }
    }
}
