package it.carlodepieri.bghq
package search

import io.lemonlabs.uri.Url

enum Status(val status: String):
  case AVAILABLE extends Status("available")
  case PREORDER extends Status("preorder")
  case UNAVAILABLE extends Status("unavailable")

abstract class GameEntry {
  val url: Url
  val store: Url
  val title: String
  val image: Url

  val availableStatus: Status
  val available: Boolean = availableStatus != Status.UNAVAILABLE

  val lang: Option[String]

  val price: Int
  val discountedPrice: Option[Int]
  val discount: Option[Int]
  val discountEndDate: Option[Int]
}

case class UnavailableEntry(
    url: Url,
    store: Url,
    title: String,
    image: Url,
    lang: Option[String] = None
) extends GameEntry {

  override val availableStatus: Status = Status.UNAVAILABLE

  override val price: Int = throw NoSuchFieldException()
  override val discountedPrice: Option[Int] = throw NoSuchFieldException()
  override val discount: Option[Int] = throw NoSuchFieldException()
  override val discountEndDate: Option[Int] = throw NoSuchFieldException()
}

case class Entry(
    url: Url,
    store: Url,
    title: String,
    image: Url,
    price: Int,
    availableStatus: Status,
    discountedPrice: Option[Int] = None,
    discount: Option[Int] = None,
    discountEndDate: Option[Int] = None,
    lang: Option[String] = None
) extends GameEntry {}

object GameEntry {
  def apply(
      url: Url,
      store: Url,
      title: String,
      image: Url,
      price: Int,
      availableStatus: Option[Status] = None,
      discountedPrice: Option[Int] = None,
      discount: Option[Int] = None,
      discountEndDate: Option[Int] = None,
      lang: Option[String] = None
  ): GameEntry = Entry(
    url,
    store,
    title,
    image,
    price,
    availableStatus match
      case Some(s) => s
      case None    => Status.AVAILABLE
    ,
    discountedPrice,
    discount,
    discountEndDate,
    lang
  )
  def unavailable(
      url: Url,
      store: Url,
      title: String,
      image: Url,
      lang: Option[String] = None
  ): GameEntry = UnavailableEntry(
    url,
    store,
    title,
    image,
    lang
  )
}
