package it.carlodepieri.bghq
package search

import io.lemonlabs.uri.Url
import zio.*
import zio.test.*
import org.mockito.Mockito.{mock, when}
import zio.mock.Expectation
import mocks.*
import shared.*

import scala.util.{Failure, Success, Try}

object DungeonDiceSearchPageParserSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    suiteAll("A search on DungeonDice") {

      val getElement = getStoreElement(StoreName.DUNGEONDICE)
      val getResource = getStoreResource(StoreName.DUNGEONDICE)

      val searchQuery = "Terraforming Mars"

      val searchPageUrl1 =
        "https://www.dungeondice.it/ricerca?controller=search&s=terraforming+mars"
      val searchPageUrl2 =
        "https://www.dungeondice.it/ricerca?controller=search&page=2&s=terraforming+mars"

      val searchPageDocument1 = getResource("search")
      val searchPageDocument2 = getResource("search_2")

      val nextPage1 = Some(Url.parse(searchPageUrl2))
      val nextPage2 = None

      val results1: List[GameEntry] = RandomEntry.getList(25)
      val results2: List[GameEntry] = RandomEntry.getList(13)

      //
      test("should be able to return a stream of results from a query") {

        // mock the 2 calls that will be made to the CachedDocumentService
        val expectADocument =
          MockCachedDocumentService
            .Get(
              Assertion.equalTo(searchPageUrl1, false),
              Expectation.value(searchPageDocument1)
            ) && MockCachedDocumentService
            .Get(
              Assertion.equalTo(searchPageUrl2, false),
              Expectation.value(searchPageDocument2)
            )

        // mock the SearchPageParser
        val mockedSearch = mock(DungeonDiceSearchPageParser.getClass)
        when(mockedSearch.parseDocument(searchPageDocument1))
          .thenReturn(Success((results1, nextPage1)))
        when(mockedSearch.parseDocument(searchPageDocument2))
          .thenReturn(Success((results2, nextPage2)))
        when(mockedSearch.search(searchQuery)).thenCallRealMethod()
        when(mockedSearch.getSearchUrl(searchQuery)).thenCallRealMethod()

        // describe the ZIO that will perform the search search
        val searchEffect: Task[Chunk[Try[GameEntry]]] =
          mockedSearch
            .search(searchQuery)
            .provideLayer(
              expectADocument
            )
            .runCollect

        for {
          results <- searchEffect
        } yield assertTrue(
          results.length == results1.length + results2.length
        )
      }
        @@ TestAspect.tag("only")

      //
      test("should be able to parse a search page document") {

        val maybeResults =
          DungeonDiceSearchPageParser.parseDocument(searchPageDocument1)
        maybeResults match
          case Success((results, next)) =>
            assertTrue(
              results.length == 28,
              next == Url.parse(
                searchPageUrl2
              )
            )
            results.head match
              case Success(result) =>
                assertTrue(result.title == "Terraforming Mars")
              case Failure(e) =>
                throw e
          case Failure(e) =>
            throw e
      }

      //
      test("should be able to select search result html elements") {
        val maybeElements =
          DungeonDiceSearchPageParser.selectElements(searchPageDocument1)
        maybeElements match
          case Success(elements) =>
            assertTrue(elements.length == 28)
          case Failure(e) =>
            throw e
      }

      //
      test("should be able to parse a search result element") {
        val element = getElement(ElementName.AVAILABLE)
        val resultTry = DungeonDiceSearchPageParser.parseElement(element)
        resultTry match
          case Success(result) =>
            assertTrue(
              result.available,
              result.availableStatus == Status.AVAILABLE,
              result.url == Url.parse(
                "https://www.dungeondice.it/24805-terraforming-mars-big-box.html"
              ),
              result.store == Url.parse("dungeondice.it"),
              result.title == "Terraforming Mars - Big Box",
              result.image == Url.parse(
                "https://img.dungeondice.it/43005-home_default/terraforming-mars-big-box.jpg"
              ),
              result.price.contains(14999),
              result.discount.isEmpty,
              result.originalPrice.isEmpty,
              result.discountEndDate.isEmpty,
              result.lang.isEmpty
            )
          case Failure(e) =>
            throw e
      }

      //
      test("should be able to parse a discounted search result") {
        val element = getElement(ElementName.DISCOUNT)
        val resultTry = DungeonDiceSearchPageParser.parseElement(element)
        resultTry match
          case Success(result) =>
            assertTrue(
              result.discount.contains(20),
              result.originalPrice.contains(4499)
            )
          case Failure(e) =>
            throw e
      }

      //
      test("should be able to parse a timed discounted search result") {

        val element = getElement(ElementName.TIMER)
        val resultTry = DungeonDiceSearchPageParser.parseElement(element)
        resultTry match
          case Success(result) =>
            assertTrue(
              result.discountEndDate.nonEmpty
            )
          case Failure(e) =>
            throw e
      }

      //
      test("should be able to parse a preorder search result") {

        val element = getElement(ElementName.PREORDER)
        val resultTry = DungeonDiceSearchPageParser.parseElement(element)
        resultTry match
          case Success(result) =>
            assertTrue(result.availableStatus == Status.PREORDER)
          case Failure(e) =>
            throw e
      }

      //
      test("should be able to parse an unavailable search result") {
        val element = getElement(ElementName.UNAVAILABLE)
        val resultTry = DungeonDiceSearchPageParser.parseElement(element)
        resultTry match
          case Success(result) =>
            assertTrue(
              !result.available,
              result.availableStatus == Status.UNAVAILABLE
            )
          case Failure(e) =>
            throw e
      }

      //
      test("should recognize if a next page is available") {

        assertTrue(
          DungeonDiceSearchPageParser.nextPage(searchPageDocument1) match
            case Success(optionLink) =>
              optionLink match
                case Some(link) =>
                  link == Url.parse(
                    searchPageUrl2
                  )
                case None => false
            case Failure(e) =>
              throw e
          ,
          DungeonDiceSearchPageParser.nextPage(searchPageDocument2) match
            case Success(optionLink) =>
              optionLink match
                case Some(_) => false
                case None    => true
            case Failure(e) =>
              throw e
        )
      }

      //
      test("should be able to compose a search url from a query") {
        assertTrue(
          DungeonDiceSearchPageParser.getSearchUrl(
            searchQuery
          ) == searchPageUrl1
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

class DungeonDiceSearchPageParserJUnitSpec extends JUnitRunnableSpec {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    DungeonDiceSearchPageParserSpec.spec
}
