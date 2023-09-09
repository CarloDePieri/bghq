package it.carlodepieri.bghq

import scala.util.Try

import net.ruippeixotog.scalascraper.model.Element

trait EntryFactory {
  def buildEntry(el: Element): Try[GameEntry]
  def elementParser(el: Element): ElementParser
}
