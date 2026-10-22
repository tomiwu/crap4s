package crap4s.cli

import crap4s.core.Error
import crap4s.core.ReportWriter
import crap4s.output.AgentWriter
import crap4s.output.HumanWriter

/** Maps the values of command line flags to implementations. */
private[crap4s] object Registry:
  val DefaultFormat: String = "agent"

  val writers: Map[String, Boolean => ReportWriter] =
    Map("agent" -> (_ => AgentWriter), "human" -> (color => HumanWriter(color)))

  def writer(name: String, color: Boolean = false): Either[Error, ReportWriter] =
    writers
      .get(name)
      .map(_(color))
      .toRight(Error.UnknownFormat(name, writers.keys.toList.sorted))
