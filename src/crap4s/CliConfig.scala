package crap4s

final case class CliConfig(
    tastyDir: String,
    coverageDir: String,
    threshold: Double,
    all: Boolean
):
  def debugLine: String =
    s"tasty=$tastyDir coverage=$coverageDir threshold=$threshold all=$all"
