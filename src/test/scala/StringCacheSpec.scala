package it.carlodepieri.bghq

import zio._
import zio.test._
import zio.redis._

import shared._
import mocks._

object StringCacheSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    suiteAll("A string cache service should") {

      test("allow to cache a value") {
        for {
          redisTest <- ZIO.service[Redis]
          result <- StringCache.set(MockBrowser.url, MockBrowser.documentString)
          found <- redisTest.get(MockBrowser.safeUrl).returning[String]
        } yield {
          assertTrue(result)
          assertTrue(found.get == MockBrowser.documentStringEncoded)
        }
      }
        .provide(RedisTestLayer, RedisStringCache.layer)

      test("allow to read a cached value") {
        for {
          redisTest <- ZIO.service[Redis]
          _ <- redisTest.set(
            MockBrowser.safeUrl,
            MockBrowser.documentStringEncoded,
            expireTime = Some(1.hour)
          )
          cachedString <- StringCache.get(MockBrowser.url)
        } yield {
          val cs = cachedString.get
          assertTrue(
            cs.value == MockBrowser.documentString,
            cs.ttl.contains(1.hour),
            // TODO improve this
            cs.createdAt.nonEmpty
          )
        }
      }
        .provide(RedisTestLayer, RedisStringCache.layer)

      test("should not break when a ttl is not set") {
        for {
          redisTest <- ZIO.service[Redis]
          _ <- redisTest.set(
            MockBrowser.safeUrl,
            MockBrowser.documentStringEncoded
          )
          cachedString <- StringCache.get(MockBrowser.url)
        } yield {
          val cs = cachedString.get
          assertTrue(
            cs.value == MockBrowser.documentString,
            cs.ttl.isEmpty,
            cs.createdAt.isEmpty
          )
        }
      }
        .provide(RedisTestLayer, RedisStringCache.layer)

      test("should return Option(None) if the key is not present") {
        for {
          value <- StringCache.get("meaningOfLife")
        } yield assertTrue(value.isEmpty)
      }
        .provide(RedisTestLayer, RedisStringCache.layer)

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

class StringCacheJUnitSpec extends JUnitRunnableSpec {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    StringCacheSpec.spec
}
