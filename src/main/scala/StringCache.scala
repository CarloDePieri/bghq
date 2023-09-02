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

  private def safeKey(key: String): String = URLEncoder.encode(key, "UTF-8")

  def get(key: String): ZIO[StringCache, Throwable, Option[String]] =
    ZIO.serviceWithZIO[StringCache] {
      sc =>
        sc.get(safeKey(key)).map {
          case Some(v) => Some(Base64Encoder.decode(v))
          case None    => None
        }
    }
  def set(
      key: String,
      value: String,
      ttl: Option[Duration] = None
  ): ZIO[StringCache, Throwable, Boolean] =
    val safeValue = Base64Encoder.encode(value)
    ZIO.serviceWithZIO[StringCache](_.set(safeKey(key), safeValue, ttl))
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
