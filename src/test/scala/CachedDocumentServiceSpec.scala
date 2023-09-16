package it.carlodepieri.bghq

import shared.*
import mocks.{MockBrowser, MockCacheService, MockDocumentService}

import zio.*
import zio.test.*
import zio.mock.*

import java.time.LocalDateTime

object CachedDocumentServiceSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    suiteAll("A cached DocumentService") {

      val url = MockBrowser.url
      val document = MockBrowser.document
      val createdAt = Some(LocalDateTime.now())
      val ttl = Some(1.hour)

      //
      test("should take advantage of the cache when the page is cached") {
        val expectACacheHit =
          MockCacheService
            .Get(
              Assertion.equalTo(url),
              Expectation.value(
                Some(CachedString(MockBrowser.documentString, createdAt, ttl))
              )
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
          cachedDoc: CachedDocument <- CachedDocumentService
            .get(url)
            .provide(
              expectACacheHit,
              expectOnlyADocumentParseString,
              CachedDocumentServiceImpl.layer
            )
          out <- ZTestLogger.logOutput
        } yield assertTrue(
          cachedDoc.document.toHtml == document.toHtml,
          cachedDoc.ttl == ttl,
          cachedDoc.createdAt == createdAt.get,
          out.hasLogMessage(s"cache hit for $url")
        )
      }

      test("should ignore the cache when told to do so") {
        val expectACacheSet = MockCacheService
          .Set(
            Assertion.equalTo(
              // in here I need to specify optional argument, even if they are not actually passed in the actual call
              (url, document.toHtml, None)
            ),
            Expectation.value(true)
          )
        val expectOnlyADocumentGet =
          // It should only call the .get method.
          MockDocumentService
            .Get(
              Assertion.equalTo(url),
              Expectation.value(document)
            )

        for {
          cacheResp <- CachedDocumentService
            .get(url, forceCacheRefresh = true)
            .provide(
              expectOnlyADocumentGet,
              expectACacheSet,
              CachedDocumentServiceImpl.layer
            )
          out <- ZTestLogger.logOutput
        } yield assertTrue(
          cacheResp.document.toHtml == document.toHtml,
          cacheResp.ttl.contains(6.hours),
          out.hasLogMessage(s"cache miss for $url")
        )
      }

      test("should download a page when cache missing") {
        val expectACacheMiss = MockCacheService
          .Get(
            Assertion.equalTo(url),
            Expectation.value(None)
          )
        val expectACacheSet = MockCacheService
          .Set(
            Assertion.equalTo(
              // in here I need to specify optional argument, even if they are not actually passed in the actual call
              (url, document.toHtml, None)
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
          cacheResp <- CachedDocumentService
            .get(url)
            .provide(
              expectOnlyADocumentGet,
              expectACacheMiss
                ++ expectACacheSet,
              CachedDocumentServiceImpl.layer
            )
          out <- ZTestLogger.logOutput
        } yield assertTrue(
          cacheResp.document.toHtml == document.toHtml,
          cacheResp.ttl.contains(6.hours),
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
