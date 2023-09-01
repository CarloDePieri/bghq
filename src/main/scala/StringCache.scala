package it.carlodepieri.bghq

import zio._
import zio.redis._
import zio.schema.*
import zio.schema.codec.*

trait StringCache {
  def get(
      key: String
  ): Task[Option[String]]
  def set(
      key: String,
      value: String,
      ttl: Option[Duration] = None
  ): Task[Boolean]
}

object StringCache {
  def get(key: String): ZIO[StringCache, Throwable, Option[String]] =
    ZIO.serviceWithZIO[StringCache](_.get(key))
  def set(
      key: String,
      value: String,
      ttl: Option[Duration] = None
  ): ZIO[StringCache, Throwable, Boolean] =
    ZIO.serviceWithZIO[StringCache](_.set(key, value, ttl))
}

class RedisStringCache(redis: Redis) extends StringCache {

  override def get(key: String): IO[RedisError, Option[String]] =
    redis.get(key).returning[String]

  override def set(
      key: String,
      value: String,
      ttl: Option[zio.Duration] = None
  ): IO[RedisError, Boolean] =
    redis.set(key, value, ttl)
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
}

private object ProtobufCodecSupplier extends CodecSupplier {
  def get[A: Schema]: BinaryCodec[A] = ProtobufCodec.protobufCodec
}

val RedisLayer: ZLayer[Any, RedisError, Redis] =
  ZLayer.succeed(RedisConfig.Default) >>>
    RedisExecutor.layer.orDie ++
    ZLayer.succeed[CodecSupplier](ProtobufCodecSupplier) >>>
    Redis.layer

val RedisStringCacheServiceLayer: ZLayer[Any, RedisError, StringCache] =
  RedisLayer >>> RedisStringCache.layer
