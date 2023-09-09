package it.carlodepieri.bghq
package dungeondice

import scala.util.Try

import zio.ZLayer

import net.ruippeixotog.scalascraper.model.Element

class DDEntryFactory extends EntryFactory {

  override def elementParser(el: Element): ElementParser = DDElementParser(el)

  override def buildEntry(el: Element): Try[GameEntry] =
    val ep = elementParser(el)
    for {
      status <- ep.status
      price <- ep.price
      url <- ep.url
      store <- ep.store
      image <- ep.image
      title <- ep.title
    } yield {
      if (status == Status.AVAILABLE || status == Status.PREORDER) {
        Entry(
          url,
          store,
          title,
          image,
          status,
          price,
          ep.discount,
          ep.originalPrice,
          ep.discountEndDate,
          ep.lang
        )
      } else
        UnavailableEntry(url, store, title, image, ep.lang)
    }
}

object DDEntryFactory {
  def apply: EntryFactory = new DDEntryFactory()
  def layer: ZLayer[Any, Nothing, EntryFactory] = ZLayer.succeed(apply)
}
