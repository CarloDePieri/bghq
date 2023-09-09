package it.carlodepieri.bghq
package dungeondice

import scala.util.Try
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import io.lemonlabs.uri.Url
import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.dsl.DSL.*
import net.ruippeixotog.scalascraper.dsl.DSL.Extract.*

case class DDElementParser(override val el: Element) extends ElementParser(el) {

  def status: Try[Status] = Try {
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
  }

  def price: Try[Option[Int]] = Try {
    Some(
      (el >> element(
        "div div.e-list-product-price.e-row.v-flex-middle span"
      ))
        .attr("content")
        .toSafePrice
    )
  }

  def url: Try[Url] = Try {
    Url.parse(
      (el >> element(
        "div > a.thumbnail.product-thumbnail.e-list-product-img"
      ))
        .attr("href")
    )
  }

  def store: Try[Url] = url.map(u => Url.parse(u.apexDomain.get))

  def image: Try[Url] = Try {

    Url.parse(
      (el >> element(
        "a.thumbnail.product-thumbnail.e-list-product-img img"
      ))
        .attr("src")
    )
  }

  def title: Try[String] = Try(el >> text("h3.e-list-product-title"))

  def discount: Option[Int] =
    el >?> text("span.discount-percentage") match {
      case Some(v) =>
        // "(-20%)" => 20
        Some(v.replaceAll("""\(-|%\)""", "").toInt)
      case None => None
    }

  def originalPrice: Option[Int] =
    el >?> text("span.regular-price") match {
      case Some(v) =>
        // 44,99&nbsp;€ => 4499
        Some(v.replace(" €", "").replace(",", ".").toSafePrice)
      case None => None
    }

  def discountEndDate: Option[LocalDateTime] =
    el >?> element("div.a-countdown") match {
      case Some(v) =>
        val dataString = v.attr("data-end")
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
        val parsedDateTime = LocalDateTime.parse(dataString, formatter)
        Some(parsedDateTime)
      case None => None
    }

  def lang: Option[String] = None
}
