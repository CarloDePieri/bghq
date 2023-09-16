package it.carlodepieri.bghq
package mocks

import io.lemonlabs.uri.Url
import org.scalacheck.Gen

import java.time.LocalDateTime
import scala.util.Random

object RandomEntry {

  val entryGen: Gen[Entry] = for {
    url <- Gen.choose(0, 100).map(id => Url.parse(s"https://example.com/$id"))
    store <- Gen.const(Url.parse("https://example.com"))
    title <- Gen.const(Random.alphanumeric.take(10).mkString)
    image <- Gen
      .choose(0, 100)
      .map(id => Url.parse(s"https://example.com/image/$id"))
    availableStatus <- Gen.oneOf(List(Status.AVAILABLE, Status.PREORDER))
    price <- Gen.choose(0, 200).map(Some(_))
  } yield Entry(
    url,
    store,
    title,
    image,
    availableStatus,
    LocalDateTime.now(),
    price = price
  )

  def sample: Entry = entryGen.sample.get

  def getList(n: Int): List[Entry] =
    (1 to n).map(_ => RandomEntry.sample).toList
}
