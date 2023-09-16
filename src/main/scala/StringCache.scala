package it.carlodepieri.bghq

import utils.Base64Encoder

import zio.*
import zio.redis.*
import zio.schema.*
import zio.schema.codec.*

import java.net.URLEncoder
import java.time.LocalDateTime

/**
 * Represent a value returned by a cache.
 *
 * @param value the actual string
 * @param createdAt (optional) the time this value was cached
 * @param ttl (optional) the time to live of this value
 */
case class CachedString(
    value: String,
    createdAt: Option[LocalDateTime] = None,
    ttl: Option[Duration] = None
)

trait StringCache {
  def get(
      key: String
  ): Task[Option[CachedString]]
  def set(
      key: String,
      value: String,
      ttl: Option[Duration] = Some(6.hours)
  ): Task[Boolean]
}

object StringCache {

  def get(
      key: String
  ): ZIO[StringCache, Throwable, Option[CachedString]] =
    ZIO.serviceWithZIO[StringCache](_.get(key))

  def set(
      key: String,
      value: String,
      ttl: Option[Duration] = Some(6.hours)
  ): ZIO[StringCache, Throwable, Boolean] =
    ZIO.serviceWithZIO[StringCache](_.set(key, value, ttl))
}

//
//  Implementation
//

class RedisStringCache(redis: Redis) extends StringCache {

  private def safeKey(key: String): String = URLEncoder.encode(key, "UTF-8")

  override def get(
      key: String
  ): IO[RedisError, Option[CachedString]] =
    for {
      value <- redis
        .get(safeKey(key))
        .returning[String]
        .map {
          maybeValue =>
            maybeValue.map(Base64Encoder.decode)
        }
        .catchAll(
          e =>
            ZIO.logError(s"REDIS ERROR: $e") *>
              ZIO.succeed(None)
        )
      maybeTTL: Option[Duration] <- redis
        .ttl(safeKey(key))
        .map(value => Some(value))
        .catchAll(_ => ZIO.succeed(None))
      now <- Clock.currentDateTime
    } yield {
      val createdAt = maybeTTL
        .map(ttl => now.minus(6.hours).plus(ttl).toLocalDateTime)
      value.map(CachedString(_, createdAt, maybeTTL))
    }

  override def set(
      key: String,
      value: String,
      ttl: Option[zio.Duration] = Some(6.hours)
  ): IO[RedisError, Boolean] =
    val safeValue = Base64Encoder.encode(value)
    redis.set(safeKey(key), safeValue, ttl)
}

object RedisStringCache {
  def apply(redis: Redis): RedisStringCache =
    new RedisStringCache(redis)
  def layer: ZLayer[Redis, RedisError, StringCache] =
    ZLayer {
      for {
        redis <- ZIO.service[Redis]
      } yield apply(redis)
    }
  val redisLayer: ZLayer[Any, RedisError.IOError, Redis] =
    ZLayer.succeed(RedisConfig.Default) >>>
      RedisExecutor.layer ++ ZLayer
        .succeed[CodecSupplier](ProtobufCodecSupplier)
      >>> Redis.layer
}

private object ProtobufCodecSupplier extends CodecSupplier {
  def get[A: Schema]: BinaryCodec[A] = ProtobufCodec.protobufCodec
}
