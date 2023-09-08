package it.carlodepieri.bghq
package shared

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.model.{Document, Element}
import zio.*
import zio.redis.*
import zio.redis.embedded.EmbeddedRedis
import zio.test.*

val RedisTestLayer: ZLayer[Any, RedisError, Redis] =
  EmbeddedRedis.layer.orDie >>>
    RedisExecutor.layer.orDie ++
    ZLayer.succeed[CodecSupplier](ProtobufCodecSupplier) >>>
    Redis.layer

extension (outputChunk: Chunk[ZTestLogger.LogEntry])
  def hasLogMessage(message: String): Boolean =
    outputChunk.map(_.message()).exists(_.contains(message))

enum StoreName(val name: String):
  case DUNGEONDICE extends StoreName("dungeondice")

enum ElementName(val name: String):
  case AVAILABLE extends ElementName("available")
  case DISCOUNT extends ElementName("discount")
  case PREORDER extends ElementName("preorder")
  case TIMER extends ElementName("timer")
  case UNAVAILABLE extends ElementName("unavailable")

def getStoreElement(storeName: StoreName)(
    elementName: ElementName
): Element =
  getStoreResource(storeName)(s"element_${elementName.name}").root

def getStoreResource(storeName: StoreName)(resourceName: String): Document =
  JsoupBrowser().parseResource(s"/${storeName.name}/$resourceName.html")
