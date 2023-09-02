package it.carlodepieri.bghq

import zio._
import net.ruippeixotog.scalascraper.model.Document

trait CachedDocumentService {
  def get(url: String, skipCache: Boolean = false): Task[Document]
}

object CachedDocumentService {
  def get(
      url: String,
      skipCache: Boolean = false
  ): ZIO[CachedDocumentService, Throwable, Document] =
    ZIO.serviceWithZIO[CachedDocumentService](_.get(url, skipCache))
}

class CachedDocumentServiceImpl(
    documentService: DocumentService,
    cacheService: StringCache
) extends CachedDocumentService {
  override def get(
      url: String,
      skipCache: Boolean = false
  ): Task[Document] =
    for {
      // first try to recover it from the cache
      valueFromCache <- cacheService.get(url)
      value <- valueFromCache match
        case Some(v) =>
          // cache hit, return the Document from that cached string
          ZIO.log(s"cache hit for $url") *>
            documentService.parseString(v)
        case None =>
          // cache miss
          for {
            _ <- ZIO.log(s"cache miss for $url")
            // download the page
            page <- documentService.get(url)
            // add it to the cache
            _ <- cacheService.set(url, page.toHtml)
          } yield page
    } yield value
}

object CachedDocumentServiceImpl {
  def apply(
      documentService: DocumentService,
      cacheService: StringCache
  ): CachedDocumentServiceImpl =
    new CachedDocumentServiceImpl(documentService, cacheService)
  def layer: ZLayer[
    StringCache with DocumentService,
    Nothing,
    CachedDocumentService
  ] = ZLayer {
    for {
      ds <- ZIO.service[DocumentService]
      cs <- ZIO.service[StringCache]
    } yield apply(ds, cs)
  }
}
