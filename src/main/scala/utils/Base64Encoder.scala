package it.carlodepieri.bghq
package utils

import java.util.Base64

object Base64Encoder {

  def encode(input: String): String = {
    val encodedBytes = Base64.getEncoder.encode(input.getBytes("UTF-8"))
    new String(encodedBytes, "UTF-8")
  }

  def decode(input: String): String = {
    val decodedBytes = Base64.getDecoder.decode(input.getBytes("UTF-8"))
    new String(decodedBytes, "UTF-8")
  }
}
