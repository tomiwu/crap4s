package crap4s.pipeline

import crap4s.core.Error

/** The full flow: load the inputs, map, score and report. */
def pipeline: (Units, Coverage, Mapping, Scoring, Report) ?=> Either[Error, Result] =
  Inputs.load
    .via(mapping)
    .via(scoring)
    .via(report)
