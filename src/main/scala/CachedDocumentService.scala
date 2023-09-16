package it.carlodepieri.bghq

import zio.*
import net.ruippeixotog.scalascraper.model.Document
import zio.redis.RedisError

import java.time.LocalDateTime

/**
 * Represent a cached document.
 *
 * @param document the actual document
 * @param createdAt the time this document was created (if it was cached, may be some time ago)
 * @param ttl (optional) the time to live of this value, if it was cached
 */
case class CachedDocument(
    document: Document,
    createdAt: LocalDateTime = LocalDateTime.now(),
    ttl: Option[Duration] = None
)

trait CachedDocumentService {
  def get(
      url: String,
      forceCacheRefresh: Boolean = false
  ): Task[CachedDocument]
}

object CachedDocumentService {
  def get(
      url: String,
      forceCacheRefresh: Boolean = false
  ): ZIO[CachedDocumentService, Throwable, CachedDocument] =
    ZIO.serviceWithZIO[CachedDocumentService](_.get(url, forceCacheRefresh))
}

class CachedDocumentServiceImpl(
    documentService: DocumentService,
    cacheService: StringCache
) extends CachedDocumentService {
  override def get(
      url: String,
      forceCacheRefresh: Boolean = false
  ): Task[CachedDocument] =
    for {
      maybeCachedString: Option[CachedString] <-
        if (forceCacheRefresh)
          // cache will be skipped
          ZIO.succeed(None)
        else
          // first try to recover it from the cache
          cacheService.get(url)
      value: CachedDocument <- maybeCachedString match
        case Some(cachedString) =>
          // cache hit, return the Document from that cached string
          ZIO.log(s"cache hit for $url") *>
            documentService
              .parseString(cachedString.value)
              .map(
                doc =>
                  cachedString.createdAt match
                    case Some(t) => CachedDocument(doc, t, cachedString.ttl)
                    case None    => CachedDocument(doc, ttl = cachedString.ttl)
              )
        case None =>
          // cache miss
          for {
            _ <- ZIO.log(s"cache miss for $url")
            // download the page
            page <- documentService.get(url)
            // add it to the cache
            _ <- cacheService.set(url, page.toHtml)
          } yield CachedDocument(page, ttl = Some(6.hours))
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
  def layerDefault: ZLayer[Any, RedisError, CachedDocumentService] =
    RedisStringCache.redisLayer >>>
      RedisStringCache.layer ++ JSoupDocumentService.layer >>>
      CachedDocumentServiceImpl.layer
}
