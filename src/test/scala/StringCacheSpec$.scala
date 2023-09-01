package it.carlodepieri.bghq

import zio._
import zio.test._
import zio.redis._

import shared._

object StringCacheSpec$ extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("A string cache service should")(
      //
      test("allow to cache a value") {
        for {
          redisTest <- ZIO.service[Redis]
          result <- StringCache.set("meaningOfLife", "42")
          found <- redisTest.get("meaningOfLife").returning[String]
        } yield {
          assertTrue(result)
          assertTrue(found.get == "42")
        }
      }
        .provide(RedisTestLayer, RedisStringCache.layer),
      //
      test("... allow to read a cached value") {
        for {
          redisTest <- ZIO.service[Redis]
          _ <- redisTest.set("meaningOfLife", "42")
          value <- StringCache.get("meaningOfLife")
        } yield assertTrue(value.get == "42")
      }
        .provide(RedisTestLayer, RedisStringCache.layer),
      //
      test("should return Option(None) if the key is not present") {
        for {
          value <- StringCache.get("meaningOfLife")
        } yield assertTrue(value.isEmpty)
      }
        .provide(RedisTestLayer, RedisStringCache.layer)
    ) @@ TestAspect.silentLogging
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

class StringCacheJUnitSpec extends JUnitRunnableSpec {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    StringCacheSpec$.spec
}
