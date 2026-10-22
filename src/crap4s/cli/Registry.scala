package crap4s.cli

import crap4s.output.AgentWriter
import crap4s.output.HumanWriter
import crap4s.pipeline.Report

/** Maps the values of command line flags to implementations. */
object Registry:
  def report(format: Format, color: Boolean = false): Report =
    format match
      case Format.Agent => AgentWriter.report
      case Format.Human => HumanWriter.report(color)
