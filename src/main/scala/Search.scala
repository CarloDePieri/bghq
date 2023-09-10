package it.carlodepieri.bghq

import scala.util.Try

import zio.*
import zio.stream.ZStream

import io.lemonlabs.uri.Url

trait Search {
  def search(
      query: String,
      skipCache: Boolean = false
  ): ZStream[CachedDocumentService, Throwable, Try[GameEntry]]
  def getSearchUrl(query: String): Try[Url]
}

object Search {

  // TODO
  def search(
      query: String,
      skipCache: Boolean = false
  ): ZIO[Search, Nothing, ZStream[CachedDocumentService, Throwable, Try[
    GameEntry
  ]]] =
    ZIO.serviceWith[Search](_.search(query, skipCache))

  // TODO
  def getSearchUrl(query: String): ZIO[Search, Nothing, Try[Url]] =
    ZIO.serviceWith[Search](_.getSearchUrl(query))
}
