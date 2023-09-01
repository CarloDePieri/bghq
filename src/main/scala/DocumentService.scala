package it.carlodepieri.bghq

import zio.*
import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import net.ruippeixotog.scalascraper.model.Document

trait DocumentService {
  def get(url: String): Task[Document]
  def parseString(html: String): Task[Document]
}

object DocumentService {
  def get(url: String): ZIO[DocumentService, Throwable, Document] =
    ZIO.serviceWithZIO[DocumentService](_.get(url))
  def parseString(html: String): ZIO[DocumentService, Throwable, Document] =
    ZIO.serviceWithZIO[DocumentService](_.parseString(html))
}

class JSoupDocumentService(private val browser: Browser)
    extends DocumentService {

  def get(url: String): Task[Document] =
    ZIO.log(s"Downloading $url") *>
      ZIO.attempt(browser.get(url))

  def parseString(html: String): Task[Document] =
    ZIO.log(s"Parsing html page") *>
      ZIO.attempt(browser.parseString(html))
}

object JSoupDocumentService {
  def apply: DocumentService = new JSoupDocumentService(JsoupBrowser())
  def layer: ZLayer[Any, Nothing, DocumentService] = ZLayer.succeed(apply)
}
