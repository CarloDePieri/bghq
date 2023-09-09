package it.carlodepieri.bghq

import scala.util.Try

import zio._

import net.ruippeixotog.scalascraper.model.Element

trait EntryFactory {
  def buildEntry(el: Element): Try[GameEntry]
  def elementParser(el: Element): ElementParser
}

object EntryFactory {
  def buildEntry(el: Element): ZIO[EntryFactory, Nothing, Try[GameEntry]] =
    ZIO.serviceWith[EntryFactory](_.buildEntry(el))

  def elementParser(el: Element): ZIO[EntryFactory, Nothing, ElementParser] =
    ZIO.serviceWith[EntryFactory](_.elementParser(el))
}
