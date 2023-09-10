package it.carlodepieri.bghq
package dungeondice

import shared.{StoreName, StoreResource}

import zio.*
import zio.test._
import zio.internal.stacktracer.SourceLocation

import io.lemonlabs.uri.Url

object DDElementParserSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suiteAll("A DungeonDice element parser") {

      val storeResource = StoreResource(StoreName.DUNGEONDICE)

      test("should be able to parse an element") {
        for {
          entryTry <- ElementParser.parse(storeResource.elements.available)
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
            result.lang.isEmpty
          )
        }
      }

      test(
        "can parse single element fields via the low level Parser implementation"
      ) {
        for {
          parser <- ElementParser.getParser(storeResource.elements.available)
        } yield assertTrue(parser.title.get == "Terraforming Mars - Big Box")
      }

      test("should be able to parse a discounted element") {
        for {
          entryTry <- ElementParser.parse(storeResource.elements.discount)
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
          entryTry <- ElementParser.parse(storeResource.elements.timer)
        } yield assertTrue(
          entryTry.get.discountEndDate.nonEmpty
        )
      }

      test("should be able to parse an unavailable element") {
        for {
          entryTry <- ElementParser.parse(
            storeResource.elements.unavailable
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
          entryTry <- ElementParser.parse(storeResource.elements.preorder)
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
