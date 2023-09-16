package it.carlodepieri.bghq
package dungeondice

import shared.{StoreName, StoreResource}

import zio.*
import zio.test.*
import zio.internal.stacktracer.SourceLocation
import io.lemonlabs.uri.Url

import java.time.LocalDateTime

object DDElementParserSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suiteAll("A DungeonDice element parser") {

      val storeResource = StoreResource(StoreName.DUNGEONDICE)
      val pageDocument = CachedDocument(
        storeResource.resource("search"),
        LocalDateTime.now(),
        Some(6.hours)
      )

      test("should be able to parse an element") {
        for {
          entryTry <- ElementParser.parse(
            storeResource.elements.available,
            pageDocument
          )
        } yield {

          val result = entryTry.get
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
            result.lang.isEmpty,
            result.pageCreatedOn.isBefore(LocalDateTime.now()),
            result.pageTTL.contains(6.hours)
          )
        }
      }

      test(
        "can parse single element fields via the low level Parser implementation"
      ) {
        for {
          parser <- ElementParser.getParser(
            storeResource.elements.available,
            pageDocument
          )
        } yield assertTrue(
          parser.title.get == "Terraforming Mars - Big Box",
          parser.document.ttl.nonEmpty
        )
      }

      test("should be able to parse a discounted element") {
        for {
          entryTry <- ElementParser.parse(
            storeResource.elements.discount,
            pageDocument
          )
        } yield {
          val result = entryTry.get
          assertTrue(
            result.discount.contains(20),
            result.originalPrice.contains(4499)
          )
        }
      }

      test("should be able to parse a timed discounted element") {
        for {
          entryTry <- ElementParser.parse(
            storeResource.elements.timer,
            pageDocument
          )
        } yield assertTrue(
          entryTry.get.discountEndDate.nonEmpty
        )
      }

      test("should be able to parse an unavailable element") {
        for {
          entryTry <- ElementParser.parse(
            storeResource.elements.unavailable,
            pageDocument
          )
        } yield {
          val result = entryTry.get
          assertTrue(
            !result.available,
            result.availableStatus == Status.UNAVAILABLE
          )
        }
      }

      test("should be able to parse a preorder element") {
        for {
          entryTry <- ElementParser.parse(
            storeResource.elements.preorder,
            pageDocument
          )
        } yield assertTrue(
          entryTry.get.availableStatus == Status.PREORDER
        )
      }

    }
      .provide(DDElementParser.layer)
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

class DDElementParserJUnitSpec extends JUnitRunnableSpec {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    DDElementParserSpec.spec
}
