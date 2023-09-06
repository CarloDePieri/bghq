package it.carlodepieri.bghq
package search

import shared.{ElementName, StoreName, *}
import shared.ElementName.*
import shared.StoreName.*

import io.lemonlabs.uri.Url
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import zio.*
import zio.test.*

import scala.util.{Failure, Success}

object DungeonDiceSearchPageParserSpec$ extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    suiteAll("A search on DungeonDice") {

      val getElement = getStoreElement(StoreName.DUNGEONDICE)

      //
      test("should be able to parse a search page document") {
        val document =
          JsoupBrowser().parseResource("/dungeondice/search.html")

        val maybeResults = DungeonDiceSearchPageParser.parseDocument(document)
        maybeResults match
          case Success(results) =>
            assertTrue(results.length == 28)
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
        val document =
          JsoupBrowser().parseResource("/dungeondice/search.html")
        val maybeElements = DungeonDiceSearchPageParser.selectElements(document)
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

        val document =
          JsoupBrowser().parseResource("/dungeondice/search.html")
        val document_2 =
          JsoupBrowser().parseResource("/dungeondice/search_2.html")

        assertTrue(
          DungeonDiceSearchPageParser.nextPage(document) match
            case Success(optionLink) =>
              optionLink match
                case Some(link) =>
                  link == Url.parse(
                    "https://www.dungeondice.it/ricerca?controller=search&page=2&s=terraforming+mars"
                  )
                case None => false
            case Failure(e) =>
              throw e
          ,
          DungeonDiceSearchPageParser.nextPage(document_2) match
            case Success(optionLink) =>
              optionLink match
                case Some(_) => false
                case None    => true
            case Failure(e) =>
              throw e
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
    DungeonDiceSearchPageParserSpec$.spec
}
