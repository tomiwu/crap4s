package crap4s

final case class CliConfig(
    tastyDir: String,
    coverageDir: String,
    threshold: Double,
    all: Boolean
)

object CliConfig:
  val DefaultThreshold: Double = 6.0
