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

trait Elements {
  def available: Element
  def discount: Element
  def preorder: Element
  def timer: Element
  def unavailable: Element
}

case class StoreResource(name: StoreName) {

  val elements: Elements = new Elements {
    override def available: Element = getElement("available")
    override def discount: Element = getElement("discount")
    override def preorder: Element = getElement("preorder")
    override def timer: Element = getElement("timer")
    override def unavailable: Element = getElement("unavailable")
  }

  def getElement(elementName: String): Element =
    resource(s"element_$elementName").root

  def resource(resourceName: String): Document =
    JsoupBrowser().parseResource(s"/${name.name}/$resourceName.html")
}
