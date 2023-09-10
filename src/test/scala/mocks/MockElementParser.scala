package it.carlodepieri.bghq
package mocks

import scala.util.Try

import zio.*
import zio.mock.*

import net.ruippeixotog.scalascraper.model.Element

object MockElementParser extends Mock[ElementParser] {
  object Parse extends Effect[Element, Nothing, Try[GameEntry]]
  object GetParser extends Effect[Element, Nothing, Parser]

  override val compose: URLayer[Proxy, ElementParser] =
    ZLayer {
      for {
        proxy <- ZIO.service[Proxy]
      } yield {
        val unsafe = zio.Runtime.default.unsafe
        new ElementParser:
          override def parse(element: Element): Try[GameEntry] =
            Unsafe.unsafely {
              unsafe.run(proxy(Parse, element)).getOrThrow()
            }
          override def getParser(element: Element): Parser =
            Unsafe.unsafely {
              unsafe.run(proxy(GetParser, element)).getOrThrow()
            }
      }
    }
}
