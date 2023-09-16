package it.carlodepieri.bghq
package dungeondice

import shared.*
import mocks.{MockCachedDocumentService, MockElementParser, RandomEntry}

import io.lemonlabs.uri.Url
import zio.*
import zio.test.*
import zio.mock.*
import org.mockito.Mockito.{mock, when}

import scala.util.{Success, Try}
import net.ruippeixotog.scalascraper.dsl.DSL.*
import net.ruippeixotog.scalascraper.dsl.DSL.Extract.*
import net.ruippeixotog.scalascraper.model.Element
import zio.stream.ZStream

import java.time.LocalDateTime

object DDPageParserSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    suiteAll("A dungeondice page parser") {

      val storeResource = StoreResource(StoreName.DUNGEONDICE)
      val searchPageDocument1 = storeResource.resource("search")
      val searchPageDocument2 = storeResource.resource("search_2")

      val searchPageUrl1 =
        "https://www.dungeondice.it/ricerca?controller=search&s=terraforming+mars"
      val searchPageUrl2 =
        "https://www.dungeondice.it/ricerca?controller=search&page=2&s=terraforming+mars"

      val searchResults1: List[Try[GameEntry]] =
        RandomEntry.getList(28).map(Success(_))
      val searchResults2: List[Try[GameEntry]] =
        RandomEntry.getList(13).map(Success(_))

      val cachedDocument1 =
        CachedDocument(searchPageDocument1, LocalDateTime.now(), Some(6.hours))
      val cachedDocument2 =
        CachedDocument(searchPageDocument2, LocalDateTime.now(), Some(6.hours))

      test("should be able to parse a single element") {
        val element = storeResource.elements.available
        val entrySample = RandomEntry.sample
        val mockedElementParser =
          MockElementParser
            .Parse(
              Assertion.equalTo(element, cachedDocument1),
              Expectation.value(Success(entrySample))
            )
            .atLeast(1)

        for {
          entryTry <-
            PageParser
              .parseElement(element, cachedDocument1)
              .provide(
                mockedElementParser,
                DDPageParser.layer
              )
        } yield assertTrue {
          entryTry.get == entrySample
        }
      }

      test("should be able to return a list of its elements") {
        for {
          elementsTry <-
            PageParser
              .selectElements(searchPageDocument1)
              .provide(
                DDElementParser.layer,
                DDPageParser.layer
              )
        } yield assertTrue(
          elementsTry.get.length == 28,
          elementsTry.get.head >> text(
            "h3.e-list-product-title"
          ) == "Terraforming Mars",
          elementsTry.get.tail.head >> text(
            "h3.e-list-product-title"
          ) == "Terraforming Mars - Ares Expedition"
        )
      }

      test("should be able to return the next page url, if present") {
        for {
          nextPageTry <-
            PageParser
              .nextPage(searchPageDocument1)
              .provide(
                DDElementParser.layer,
                DDPageParser.layer
              )
        } yield assertTrue(
          nextPageTry.get.get.toString == searchPageUrl2
        )
      }

      test("should be able to parse a whole page") {
        val entrySample = RandomEntry.sample
        val mockedElementParser =
          MockElementParser
            .Parse(
              Assertion.anything,
              Expectation.value(Success(entrySample))
            )
            .atLeast(28)
        for {
          entriesTry <-
            PageParser
              .parsePage(cachedDocument1)
              .provide(
                mockedElementParser,
                DDPageParser.layer
              )
        } yield {
          val entries = entriesTry.get
          assertTrue(
            entries.length == 28,
            entries.head.get == entrySample
          )
        }
      }

      test("should be able to crawl from a page url") {
        // mock the 2 calls that will be made to the CachedDocumentService
        val mockedCachedDocumentService =
          MockCachedDocumentService
            .Get(
              Assertion.equalTo(searchPageUrl1, false),
              Expectation.value(cachedDocument1)
            ) && MockCachedDocumentService
            .Get(
              Assertion.equalTo(searchPageUrl2, false),
              Expectation.value(cachedDocument2)
            )

        // Create a PARTIAL mock of a DDPageParser
        val mockedPageParser = mock(classOf[DDPageParser])
        // mock .parsePage and .nextPage calls
        when(mockedPageParser.parsePage(cachedDocument1))
          .thenReturn(Success(searchResults1))
        when(mockedPageParser.parsePage(cachedDocument2))
          .thenReturn(Success(searchResults2))
        when(mockedPageParser.nextPage(searchPageDocument1))
          .thenReturn(Success(Some(Url.parse(searchPageUrl2))))
        when(mockedPageParser.nextPage(searchPageDocument2))
          .thenReturn(Success(None))
        // let through the .crawl call
        when(mockedPageParser.crawl(Url.parse(searchPageUrl1)))
          .thenCallRealMethod()
        // prepare a layer for the mocked PageParser
        val mockedPageParserLayer: ZLayer[Any, Nothing, PageParser] =
          ZLayer(ZIO.succeed(mockedPageParser))

        for {
          stream <-
            PageParser
              .crawl(Url.parse(searchPageUrl1))
              .provide(mockedPageParserLayer)
          entries <-
            stream
              .provideLayer(
                mockedCachedDocumentService.toLayer
              )
              .runCollect
        } yield assertTrue(
          entries.length == searchResults1.length + searchResults2.length
        )
      }

      test("should crawl lazily from a page url") {
        // mock ONLY ONE call to the CachedDocumentService. more will fail the test
        val mockedCachedDocumentService =
          MockCachedDocumentService
            .Get(
              Assertion.equalTo(searchPageUrl1, false),
              Expectation.value(cachedDocument1)
            )

        // Create a PARTIAL mock of a DDPageParser
        val mockedPageParser = mock(classOf[DDPageParser])
        // mock .parsePage and .nextPage calls
        when(mockedPageParser.parsePage(cachedDocument1))
          .thenReturn(Success(searchResults1))
        when(mockedPageParser.nextPage(searchPageDocument1))
          .thenReturn(Success(Some(Url.parse(searchPageUrl2))))
        // let through the .crawl call
        when(mockedPageParser.crawl(Url.parse(searchPageUrl1)))
          .thenCallRealMethod()
        // prepare a layer for the mocked PageParser
        val mockedPageParserLayer: ZLayer[Any, Nothing, PageParser] =
          ZLayer(ZIO.succeed(mockedPageParser))

        for {
          stream <-
            PageParser
              .crawl(Url.parse(searchPageUrl1))
              .provide(mockedPageParserLayer)
          entries <-
            stream
              .provideLayer(
                mockedCachedDocumentService.toLayer
              )
              .take(
                searchResults1.length
              ) // take results only up to the first page
              .runCollect
        } yield assertTrue(
          entries.length == searchResults1.length
        )
      }
    }
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

class DDPageParserJUnitSpec extends JUnitRunnableSpec {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    DDPageParserSpec.spec
}
