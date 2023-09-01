package it.carlodepieri.bghq
package utils

import zio.test._
import zio.test.Assertion._

object Base64EncoderSpec extends ZIOSpecDefault {

  def spec: Spec[Any, Nothing] = suite("A Base64EncoderDecoder")(
    test("... encoded output should not match original") {
      val original = "Hello, Encode!"
      val encodedString = Base64Encoder.encode(original)
      assertTrue(encodedString != original)
    },
    test("Decoding should match original") {
      val original = "Hello, Decode!"
      val encoded = Base64Encoder.encode(original)
      val decodedString = Base64Encoder.decode(encoded)
      assertTrue(decodedString == original)
    }
  )
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

class Base64EncoderJUnitSpec extends JUnitRunnableSpec {
  override def spec: Spec[Any, Nothing] =
    Base64EncoderSpec.spec
}
