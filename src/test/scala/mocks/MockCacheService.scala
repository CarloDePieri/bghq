package it.carlodepieri.bghq
package mocks

import zio.mock.{Mock, Proxy}
import zio.{Duration, Task, URLayer, ZIO, ZLayer}

object MockCacheService extends Mock[StringCache] {
  object Get extends Effect[String, Throwable, Option[String]]
  object Set
      extends Effect[(String, String, Option[Duration]), Throwable, Boolean]

  val compose: URLayer[Proxy, StringCache] =
    ZLayer {
      for {
        proxy <- ZIO.service[Proxy]
      } yield new StringCache {
        override def get(key: String): Task[Option[String]] =
          proxy(Get, key)

        override def set(
            key: String,
            value: String,
            ttl: Option[Duration] = None
        ): Task[Boolean] =
          proxy(Set, key, value, ttl)
      }
    }
}
