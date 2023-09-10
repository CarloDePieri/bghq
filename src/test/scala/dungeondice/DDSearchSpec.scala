package it.carlodepieri.bghq
package dungeondice

import mocks.{MockCachedDocumentService, MockPageParser, RandomEntry}

import io.lemonlabs.uri.Url
import zio.*
import zio.test.*
import org.mockito.Mockito.{mock, when}
import zio.mock.*
import zio.stream.ZStream

import scala.util.{Success, Try}

object DDSearchSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    suiteAll("A DungeonDice search") {

      val query = "Terraforming Mars"
      val searchPageUrl =
        "https://www.dungeondice.it/ricerca?controller=search&s=terraforming+mars"
      val parsedSearchUrl = Url.parse(searchPageUrl)

      test("should produce a search url from a query string") {
        val mockedPageParser = MockPageParser.empty
        for {
          urlTry <- Search
            .getSearchUrl(query)
            .provide(
              mockedPageParser,
              DDSearch.layer
            )
        } yield assertTrue(urlTry.get == parsedSearchUrl)
      }

      test("should return a stream with the results") {
        val mockedCachedDocumentService = MockCachedDocumentService.empty
        val mockedPageParser = MockPageParser
          .Crawl(
            Assertion.equalTo(parsedSearchUrl, false),
            Expectation.value(
              ZStream.fromIterable(RandomEntry.getList(35).map(Success(_)))
            )
          )

        // Create a PARTIAL mock of a DDSearch by subclassing (because I need the PageParser reference)
        val mockedSearchLayer: ZLayer[PageParser, Nothing, Search] = ZLayer {
          for {
            pp <- ZIO.service[PageParser]
          } yield new DDSearch(pp) {
            // mock .getSearchUrl
            override def getSearchUrl(query: String): Try[Url] =
              Success(parsedSearchUrl)
            // but .search will work normally!
          }
        }

        for {
          stream <-
            Search
              .search(query)
              .provide(mockedPageParser, mockedSearchLayer)
          entries <-
            stream
              .provideLayer(mockedCachedDocumentService)
              .runCollect
        } yield assertTrue(entries.length == 35)
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

class DDSearchJUnitSpec extends JUnitRunnableSpec {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    DDSearchSpec.spec
}
