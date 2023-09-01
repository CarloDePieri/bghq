package it.carlodepieri.bghq

import utils.Base64Encoder

import zio._
import net.ruippeixotog.scalascraper.model.Document
import java.net.URLEncoder

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
    val encoded_url = URLEncoder.encode(url, "UTF-8")
    for {
      // first try to recover it from the cache
      valueFromCache <- cacheService.get(encoded_url)
      value <- valueFromCache match
        case Some(v) =>
          // cache hit, return the Document from that cached string
          ZIO.log(s"cache hit for $url") *>
            documentService.parseString(Base64Encoder.decode(v))
        case None =>
          // cache miss
          for {
            _ <- ZIO.log(s"cache miss for $url")
            // download the page
            page <- documentService.get(url)
            // add it to the cache
            _ <- cacheService.set(
              encoded_url,
              Base64Encoder.encode(page.toHtml)
            )
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
