package crap4s.cli

import crap4s.core.ReportWriter
import crap4s.output.AgentWriter
import crap4s.output.HumanWriter

/** Maps the values of command line flags to implementations. */
private[cli] object Registry:
  def writer(format: Format, color: Boolean = false): ReportWriter =
    format match
      case Format.Agent => AgentWriter
      case Format.Human => HumanWriter(color)
