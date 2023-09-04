package it.carlodepieri.bghq
package search

import io.lemonlabs.uri.Url
import net.ruippeixotog.scalascraper.model.{Document, Element}
import net.ruippeixotog.scalascraper.dsl.DSL.*
import net.ruippeixotog.scalascraper.dsl.DSL.Extract.*
import zio.prelude.data.Optional.AllValuesAreNullable

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.util.Try

case class DungeonDiceElementParser(el: Element) extends ElementParser {

  def getStatus: Status =
    val addToCartButton =
      el >?> element("button[data-button-action=add-to-cart]")
    addToCartButton match {
      case Some(v) =>
        if (v.attr("class").contains("v-green"))
          Status.AVAILABLE
        else
          Status.UNAVAILABLE
      case None =>
        // try to access the preorder button
        el >> text("div > form.e-list-product-cart > a.e-item.v-blue")
        // if found, set this as a preorder
        Status.PREORDER
    }

  def getPrice: Option[Int] =
    (el >> element(
      "div div.e-list-product-price.e-row.v-flex-middle span"
    ))
      .attr("content")
      .toSafePrice
      .toOption

  def getUrl: Url =
    Url.parse(
      (el >> element(
        "div > a.thumbnail.product-thumbnail.e-list-product-img"
      ))
        .attr("href")
    )

  def getStore: Url =
    Url.parse(getUrl.apexDomain.get)

  def getImage: Url =
    Url.parse(
      (el >> element(
        "a.thumbnail.product-thumbnail.e-list-product-img img"
      ))
        .attr("src")
    )

  def getTitle: String = el >> text("h3.e-list-product-title")

  def getDiscount: Option[Int] =
    el >?> text("span.discount-percentage") match {
      case Some(v) =>
        // "(-20%)" => 20
        Some(v.replaceAll("""\(-|%\)""", "").toInt)
      case None => None
    }

  def getOriginalPrice: Option[Int] =
    el >?> text("span.regular-price") match {
      case Some(v) =>
        // 44,99&nbsp;€ => 4499
        Some(v.replace(" €", "").replace(",", ".").toSafePrice)
      case None => None
    }
  override def getDiscountEndDate: Option[LocalDateTime] =
    el >?> element("div.a-countdown") match {
      case Some(v) =>
        val dataString = v.attr("data-end")
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
        val parsedDateTime = LocalDateTime.parse(dataString, formatter)
        Some(parsedDateTime)
      case None => None
    }
}

object DungeonDiceSearch extends Search {

  override def selectElements(
      page: Document
  ): Try[List[Element]] =
    Try {
      page >> elementList("#js-product-list > div > article")
    }

  override def parseElement(el: Element): Try[GameEntry] = Try {
    val ep = DungeonDiceElementParser(el)
    val status = ep.getStatus
    val url = ep.getUrl
    val store = ep.getStore
    val title = ep.getTitle
    val image = ep.getImage
    val lang = None

    if (status == Status.AVAILABLE || status == Status.PREORDER) {
      val price = ep.getPrice
      val discount = ep.getDiscount
      val originalPrice = ep.getOriginalPrice
      val discountEndDate = ep.getDiscountEndDate
      Entry(
        url,
        store,
        title,
        image,
        status,
        price,
        discount,
        originalPrice,
        discountEndDate,
        lang
      )
    } else
      UnavailableEntry(url, store, title, image, lang)
  }
}
