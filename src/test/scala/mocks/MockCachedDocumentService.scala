package it.carlodepieri.bghq
package mocks

import zio.*
import zio.mock.*

object MockCachedDocumentService extends Mock[CachedDocumentService] {

  object Get extends Effect[(String, Boolean), Throwable, CachedDocument]

  override val compose: URLayer[Proxy, CachedDocumentService] =
    ZLayer {
      for {
        proxy <- ZIO.service[Proxy]
      } yield new CachedDocumentService {
        override def get(
            url: String,
            forceCacheRefresh: Boolean = false
        ): Task[CachedDocument] =
          proxy(Get, url, forceCacheRefresh)
      }
    }
}
