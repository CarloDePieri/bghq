package it.carlodepieri.bghq

import utils.Base64Encoder
import zio.*
import zio.redis.*
import zio.schema.*
import zio.schema.codec.*

import java.net.URLEncoder

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

  private def safeKey(key: String): String = URLEncoder.encode(key, "UTF-8")

  override def get(key: String): IO[RedisError, Option[String]] =
    redis
      .get(safeKey(key))
      .returning[String]
      .map {
        maybeValue =>
          maybeValue.map(Base64Encoder.decode)
      }

  override def set(
      key: String,
      value: String,
      ttl: Option[zio.Duration] = None
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
