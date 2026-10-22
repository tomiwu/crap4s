package crap4s.input.scoverage

import crap4s.core.Error

object MeasurementsParser:
  /** Parses the text of one `scoverage.measurements.*` file: one statement id on each line. */
  def parse(name: String, text: String): Either[Error, Set[Int]] =
    text.linesIterator.map(_.trim).filter(_.nonEmpty).zipWithIndex.foldLeft(
      Right(Set.empty): Either[Error, Set[Int]]
    ):
      case (acc, (line, index)) =>
        for
          ids <- acc
          id <- line.toIntOption.toRight(Error.BadMeasurement(name, index + 1, line))
        yield ids + id
