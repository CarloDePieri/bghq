package it.carlodepieri.bghq

import dungeondice._

import zio.*
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

import scala.util.{Failure, Success, Try}

def storeInitials(domain: String): String = domain match
  case "dungeondice.it" => "[DD]"
  case _                => "[??]"

def prettyPrint(maybeEntry: Try[GameEntry]): Unit = maybeEntry match
  case Success(entry) =>
    val price = entry.price match
      case Some(p) => s" {${p / 100}€}"
      case None    => " {unavail}"
    println(
      s"${storeInitials(
          entry.store.toString
        )} ${entry.title}$price [${entry.pageTTL.get.toString}]"
    )
  case Failure(e) => println("FAIL")

def testSearch = for {
  stream <- Search
    .search("Spirit Island", forceCacheRefresh = false)
    .map(_.provideLayer(CachedDocumentServiceImpl.layerDefault))
    .provide(DDSearch.layerDefault)
  _ <- stream
    .map(el => prettyPrint(el))
    .runCollect
} yield ()

object zioPlayground extends ZIOAppDefault {
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = testSearch
}

object playground extends App {}
