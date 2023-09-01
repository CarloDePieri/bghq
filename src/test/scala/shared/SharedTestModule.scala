package it.carlodepieri.bghq
package shared

import zio.test._

extension (outputChunk: Chunk[ZTestLogger.LogEntry])
  def hasLogMessage(message: String): Boolean =
    outputChunk.map(_.message()).exists(_.contains(message))
