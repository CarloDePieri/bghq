package it.carlodepieri.bghq

import shared._
import mocks.MockBrowser

import zio.*
import zio.test.*

import org.mockito.Mockito.when

object DocumentServiceSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    suiteAll("A document service should") {

      val mockUrl = MockBrowser.url
      val stubDocument = MockBrowser.document
      val stubDocumentString = MockBrowser.documentString
      val mockedBrowser = MockBrowser.spyBrowser

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
