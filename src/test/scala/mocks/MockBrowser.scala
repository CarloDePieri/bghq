package it.carlodepieri.bghq
package mocks

import utils.Base64Encoder
import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import net.ruippeixotog.scalascraper.model.Document
import org.mockito.Mockito.{mock, spy}

object MockBrowser {

  val url = "https://httpbin.org/html"
  // noinspection SpellCheckingInspection
  val safeUrl = "https%3A%2F%2Fhttpbin.org%2Fhtml"
  private val _browser: Browser = JsoupBrowser()
  private def getDocument: Document = _browser.parseResource("/page.html")

  def document: Document = getDocument
  def documentString: String = getDocument.toHtml
  def documentStringEncoded: String = Base64Encoder.encode(documentString)

  def spyBrowser: Browser = spy(_browser)
  def mockBrowser: Browser = mock(_browser)

}
