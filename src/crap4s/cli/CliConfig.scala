package crap4s.cli

import crap4s.core.Threshold

final case class CliConfig(
    tastyDirs: List[String],
    coverageDirs: List[String],
    classpath: List[String],
    threshold: Threshold,
    all: Boolean,
    format: Format = Format.Default
)

object CliConfig:
  val DefaultThreshold: Threshold = Threshold.Default
