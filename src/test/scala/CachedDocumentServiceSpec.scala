package it.carlodepieri.bghq

import shared._

import mocks.{MockBrowser, MockCacheService, MockDocumentService}

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
      test("should take advantage of the cache when the key is present") {
        val expectations =
          // given a cache hit...
          MockCacheService
            .Get(
              Assertion.equalTo(encodedUrl),
              Expectation.value(Some(MockBrowser.documentEncoded))
            )
            .atLeast(1)
          // it should not call the DocumentService.get method ...
            ++ MockDocumentService.empty
            // ... but the .parseString method.
            ++ MockDocumentService
              .ParseString(
                Assertion.equalTo(MockBrowser.documentString),
                Expectation.value(document)
              )
              .atLeast(1)

        for {
          doc <- CachedDocumentService
            .get(url)
            .provide(
              expectations,
              CachedDocumentServiceImpl.layer
            )
          out <- ZTestLogger.logOutput
        } yield assertTrue(
          doc.toHtml == document.toHtml,
          out.hasLogMessage(s"cache hit for $url")
        )
      }
      // TODO cache miss
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
