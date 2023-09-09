package it.carlodepieri.bghq

import scala.util.Try
import java.time.LocalDateTime

import io.lemonlabs.uri.Url
import net.ruippeixotog.scalascraper.model.Element

/**
 * Low level parser that allows to access directly single part of an Element
 */
abstract class ElementParser(val el: Element) {

  extension (s: String) def toSafePrice: Int = (s.toFloat * 100).toInt

  def status: Try[Status]
  def price: Try[Option[Int]]
  def url: Try[Url]
  def store: Try[Url]
  def image: Try[Url]
  def title: Try[String]
  def discount: Option[Int]
  def originalPrice: Option[Int]
  def discountEndDate: Option[LocalDateTime]
  def lang: Option[String]
}
