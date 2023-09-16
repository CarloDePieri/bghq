package it.carlodepieri.bghq
package dungeondice

import scala.util.Try
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import io.lemonlabs.uri.Url
import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.dsl.DSL.*
import net.ruippeixotog.scalascraper.dsl.DSL.Extract.*
import zio._

class DDElementParser extends ElementParser {
  override def getParser(
      elementToParse: Element,
      pageDocument: CachedDocument
  ): Parser =
    new Parser(
      elementToParse,
      pageDocument
    ) {

      override val status: Try[Status] = Try {
        val addToCartButton =
          htmlElement >?> element("button[data-button-action=add-to-cart]")
        addToCartButton match {
          case Some(v) =>
            if (v.attr("class").contains("v-green"))
              Status.AVAILABLE
            else
              Status.UNAVAILABLE
          case None =>
            // try to access the preorder button
            htmlElement >> text(
              "div > form.e-list-product-cart > a.e-item.v-blue"
            )
            // if found, set this as a preorder
            Status.PREORDER
        }
      }

      override val price: Try[Option[Int]] = Try {
        Some(
          (htmlElement >> element(
            "div div.e-list-product-price.e-row.v-flex-middle span"
          ))
            .attr("content")
            .toSafePrice
        )
      }

      override val url: Try[Url] = Try {
        Url.parse(
          (htmlElement >> element(
            "div > a.thumbnail.product-thumbnail.e-list-product-img"
          ))
            .attr("href")
        )
      }

      override val store: Try[Url] =
        url.map(u => Url.parse(u.apexDomain.get))

      override val image: Try[Url] = Try {
        Url.parse(
          (htmlElement >> element(
            "a.thumbnail.product-thumbnail.e-list-product-img img"
          ))
            .attr("src")
        )
      }

      override val title: Try[String] = Try(
        htmlElement >> text("h3.e-list-product-title")
      )

      override val discount: Option[Int] =
        htmlElement >?> text("span.discount-percentage") match {
          case Some(v) =>
            // "(-20%)" => 20
            Some(v.replaceAll("""\(-|%\)""", "").toInt)
          case None => None
        }

      override val originalPrice: Option[Int] =
        htmlElement >?> text("span.regular-price") match {
          case Some(v) =>
            // 44,99&nbsp;€ => 4499
            Some(v.replace(" €", "").replace(",", ".").toSafePrice)
          case None => None
        }

      override val discountEndDate: Option[LocalDateTime] =
        htmlElement >?> element("div.a-countdown") match {
          case Some(v) =>
            val dataString = v.attr("data-end")
            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
            val parsedDateTime = LocalDateTime.parse(dataString, formatter)
            Some(parsedDateTime)
          case None => None
        }

      override val lang: Option[String] = None
    }

}

object DDElementParser {
  def apply: ElementParser = new DDElementParser()
  def layer: ZLayer[Any, Nothing, ElementParser] = ZLayer.succeed(apply)
}
