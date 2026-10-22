package crap4s

final case class CliConfig(
    tastyDirs: List[String],
    coverageDirs: List[String],
    classpath: List[String],
    threshold: Threshold,
    all: Boolean
)

object CliConfig:
  val DefaultThreshold: Threshold = Threshold.Default
