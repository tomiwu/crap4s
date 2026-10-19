package crap4s.coverage

object MeasurementsParser:
  /** Parses the text of one `scoverage.measurements.*` file: one statement id on each line. */
  def parse(name: String, text: String): Either[String, Set[Int]] =
    text.linesIterator.map(_.trim).filter(_.nonEmpty).zipWithIndex.foldLeft(
      Right(Set.empty): Either[String, Set[Int]]
    ):
      case (acc, (line, index)) =>
        for
          ids <- acc
          id <- line.toIntOption.toRight(s"Damaged $name: line ${index + 1} is not a statement id: '$line'")
        yield ids + id
