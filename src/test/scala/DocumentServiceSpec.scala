package it.carlodepieri.bghq

import shared._

import zio.*
import zio.test.*
import zio.mock.*
import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import net.ruippeixotog.scalascraper.model.Document
import org.mockito.Mockito.{doReturn, mock, spy, when}

object DocumentServiceSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    suiteAll("A document service should") {

      val mockUrl = "https://httpbin.org/html"
      val browser = JsoupBrowser()
      val stubDocument = browser.parseResource("/page.html")
      val stubDocumentString = stubDocument.toHtml
      val mockedBrowser = spy(JsoupBrowser())

      //
      test("be able to download a web page") {

        when(mockedBrowser.get(mockUrl))
          .thenReturn(stubDocument)

        for {
          page <- new JSoupDocumentService(mockedBrowser).get(mockUrl)
          out <- ZTestLogger.logOutput
        } yield assertTrue(
          page == stubDocument,
          out.hasLogMessage(s"Downloading $mockUrl")
        )
      }
      //
      test("be able to parse a string") {

        when(mockedBrowser.parseString(stubDocumentString))
          .thenReturn(stubDocument)

        for {
          page <- new JSoupDocumentService(mockedBrowser).parseString(
            stubDocumentString
          )
          out <- ZTestLogger.logOutput
        } yield assertTrue(
          page.toHtml == stubDocument.toHtml,
          out.hasLogMessage("Parsing html page")
        )

      }
    }
      @@ TestAspect.tag("slow")
}

/**
 * Intellij currently can either:
 *   - Run a specific test with ZIO plugin: `object TestSpec extends ZIOSpecDefault`
 *   - Run all test in a JUnit format: `class TestSpec extends JUnitRunnableSpec`
 *
 * Something like `object TestSpec extends JUnitRunnableSpec` does not work right now.
 * To keep both options available, let's add a separate junit spec that recalls the ZIO one.
 */
import zio.test.junit.JUnitRunnableSpec

class DocumentServiceJUnitSpec extends JUnitRunnableSpec {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    DocumentServiceSpec.spec
}
