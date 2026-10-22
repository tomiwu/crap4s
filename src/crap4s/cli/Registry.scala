package crap4s.cli

import crap4s.core.Threshold
import crap4s.output.agentReport
import crap4s.output.humanReport
import crap4s.pipeline.Report

/** Maps the values of command line flags to implementations. */
def reportFor(format: Format, threshold: Threshold, all: Boolean, color: Boolean = false): Report =
  format match
    case Format.Agent => agentReport(threshold, all)
    case Format.Human => humanReport(color, threshold)
