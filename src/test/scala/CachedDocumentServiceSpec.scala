package it.carlodepieri.bghq

import shared.*
import mocks.{MockBrowser, MockCacheService, MockDocumentService}

import utils.Base64Encoder
import zio.*
import zio.test.*
import zio.mock.*

object CachedDocumentServiceSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    suiteAll("A cached DocumentService") {

      val url = MockBrowser.url
      val encodedUrl = MockBrowser.safeUrl
      val document = MockBrowser.document

      //
      test("should take advantage of the cache when the page is cached") {
        val expectACacheHit =
          MockCacheService
            .Get(
              Assertion.equalTo(encodedUrl),
              Expectation.value(Some(MockBrowser.documentEncoded))
            )
        val expectOnlyADocumentParseString =
          // it should not call the DocumentService.get method ...
          MockDocumentService.empty
          // ... but only the .parseString method.
            ++ MockDocumentService
              .ParseString(
                Assertion.equalTo(MockBrowser.documentString),
                Expectation.value(document)
              )

        for {
          doc <- CachedDocumentService
            .get(url)
            .provide(
              expectACacheHit,
              expectOnlyADocumentParseString,
              CachedDocumentServiceImpl.layer
            )
          out <- ZTestLogger.logOutput
        } yield assertTrue(
          doc.toHtml == document.toHtml,
          out.hasLogMessage(s"cache hit for $url")
        )
      }
      //
      test("should download a page when cache missing") {
        val expectACacheMiss = MockCacheService
          .Get(
            Assertion.equalTo(encodedUrl),
            Expectation.value(None)
          )
        val expectACacheSet = MockCacheService
          .Set(
            Assertion.equalTo(
              // in here I need to specify optional argument, even if they are not actually passed in the actual call
              (encodedUrl, Base64Encoder.encode(document.toHtml), None)
            ),
            Expectation.value(true)
          )
        val expectOnlyADocumentGet =
          // It should not call the DocumentService.parseString method ...
          MockDocumentService.empty
          // ... but only the .get method.
            ++ MockDocumentService
              .Get(
                Assertion.equalTo(url),
                Expectation.value(document)
              )

        for {
          doc <- CachedDocumentService
            .get(url)
            .provide(
              expectOnlyADocumentGet,
              expectACacheMiss
                ++ expectACacheSet,
              CachedDocumentServiceImpl.layer
            )
          out <- ZTestLogger.logOutput
        } yield assertTrue(
          doc.toHtml == document.toHtml,
          out.hasLogMessage(s"cache miss for $url")
        )
      }
    } @@ TestAspect.silentLogging
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

class CachedDocumentServiceJUnitSpec extends JUnitRunnableSpec {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    CachedDocumentServiceSpec.spec
}
