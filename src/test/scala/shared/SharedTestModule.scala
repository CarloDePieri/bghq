package it.carlodepieri.bghq
package shared

import zio._
import zio.redis._
import zio.redis.embedded.EmbeddedRedis

import zio.test._

val RedisTestLayer: ZLayer[Any, RedisError, Redis] =
  EmbeddedRedis.layer.orDie >>>
    RedisExecutor.layer.orDie ++
    ZLayer.succeed[CodecSupplier](ProtobufCodecSupplier) >>>
    Redis.layer

extension (outputChunk: Chunk[ZTestLogger.LogEntry])
  def hasLogMessage(message: String): Boolean =
    outputChunk.map(_.message()).exists(_.contains(message))
