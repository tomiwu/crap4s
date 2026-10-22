package crap4s.pipeline

import crap4s.core.Error

/** The full flow: load the inputs, map, score and report. */
def pipeline: (Units, Coverage, Mapping, Scoring, Report) ?=> Either[Error, Result] =
  loadInputs
    .flatMap(mapping)
    .flatMap(scoring)
    .flatMap(report)
