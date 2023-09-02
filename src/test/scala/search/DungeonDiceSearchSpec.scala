package it.carlodepieri.bghq
package search

import io.lemonlabs.uri.Url
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import zio.*
import zio.test.*

import scala.util.{Failure, Success}

object DungeonDiceSearchSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    suiteAll("A search on DungeonDice") {

      //
      test("should be able to parse a search result") {
        val document =
          JsoupBrowser().parseResource("/dungeondice_entry_available.html")
        val resultTry = DungeonDiceSearch.parseElement(document)
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

        val document =
          JsoupBrowser().parseResource(
            "/dungeondice_entry_available_discount.html"
          )
        val resultTry = DungeonDiceSearch.parseElement(document)
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

        val document =
          JsoupBrowser().parseResource(
            "/dungeondice_entry_available_timer.html"
          )
        val resultTry = DungeonDiceSearch.parseElement(document)
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

        val document =
          JsoupBrowser().parseResource(
            "/dungeondice_entry_preorder.html"
          )
        val resultTry = DungeonDiceSearch.parseElement(document)
        resultTry match
          case Success(result) =>
            assertTrue(result.availableStatus == Status.PREORDER)
          case Failure(e) =>
            throw e
      }

      //
      test("should be able to parse an unavailable search result") {
        val document =
          JsoupBrowser().parseResource(
            "/dungeondice_entry_unavailable.html"
          )
        val resultTry = DungeonDiceSearch.parseElement(document)
        resultTry match
          case Success(result) =>
            assertTrue(
              !result.available,
              result.availableStatus == Status.UNAVAILABLE
            )
          case Failure(e) =>
            throw e
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

class DungeonDiceSearchJUnitSpec extends JUnitRunnableSpec {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    DungeonDiceSearchSpec.spec
}
