package it.carlodepieri.bghq
package dungeondice

import scala.util.Try
import zio.*
import io.lemonlabs.uri.{QueryString, Url}
import zio.stream.ZStream

class DDSearch(pageParser: PageParser) extends Search {
  override def getSearchUrl(query: String): Try[Url] = Try {
    Url(
      scheme = "https",
      host = "www.dungeondice.it",
      path = "/ricerca",
      query = QueryString.fromPairs(
        "controller" -> "search",
        "s" -> query.toLowerCase
      )
    )
  }

  override def search(
                       query: String,
                       forceCacheRefresh: Boolean
  ): ZStream[CachedDocumentService, Throwable, Try[GameEntry]] =
    pageParser.crawl(
      getSearchUrl(query).get,
      forceCacheRefresh
    )
}

object DDSearch {
  def apply(pageParser: PageParser): Search = new DDSearch(pageParser)

  def layer: ZLayer[PageParser, Nothing, Search] = ZLayer {
    for {
      pp <- ZIO.service[PageParser]
    } yield apply(pp)
  }

  def layerDefault: ZLayer[Any, Nothing, Search] =
    DDElementParser.layer >>>
      DDPageParser.layer >>>
      DDSearch.layer
}
