package crap4s

final case class CliConfig(
    tastyDirs: List[String],
    coverageDirs: List[String],
    classpath: List[String],
    threshold: Double,
    all: Boolean
)

object CliConfig:
  val DefaultThreshold: Double = 6.0
