package it.carlodepieri.bghq
package search

import io.lemonlabs.uri.Url

import java.time.LocalDateTime

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
  val available: Boolean

  val lang: Option[String]

  val price: Option[Int]
  val discount: Option[Int]
  val originalPrice: Option[Int]
  val discountEndDate: Option[LocalDateTime]
}

case class UnavailableEntry(
    url: Url,
    store: Url,
    title: String,
    image: Url,
    lang: Option[String] = None
) extends GameEntry {

  val available: Boolean = false
  override val availableStatus: Status = Status.UNAVAILABLE

  override val price: Option[Int] = None
  override val discount: Option[Int] = None
  override val originalPrice: Option[Int] = None
  override val discountEndDate: Option[LocalDateTime] = None
}

case class Entry(
    url: Url,
    store: Url,
    title: String,
    image: Url,
    availableStatus: Status,
    price: Option[Int] = None,
    discount: Option[Int] = None,
    originalPrice: Option[Int] = None,
    discountEndDate: Option[LocalDateTime] = None,
    lang: Option[String] = None
) extends GameEntry {
  val available: Boolean = true
}

object GameEntry {
  def apply(
      url: Url,
      store: Url,
      title: String,
      image: Url,
      price: Option[Int] = None,
      availableStatus: Option[Status] = None,
      discount: Option[Int] = None,
      originalPrice: Option[Int] = None,
      discountEndDate: Option[LocalDateTime] = None,
      lang: Option[String] = None
  ): GameEntry = Entry(
    url,
    store,
    title,
    image,
    availableStatus match
      case Some(s) => s
      case None    => Status.AVAILABLE
    ,
    price,
    discount,
    originalPrice,
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
