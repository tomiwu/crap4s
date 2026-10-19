package crap4s

import crap4s.coverage.CoverageLoader
import crap4s.tasty.TastyLoader

import java.nio.file.Path

final case class Outcome(out: String, err: String, code: Int)

object Cli:
  val AboveThresholdCode: Int = 2
  val ErrorCode: Int = 1

  def run(args: List[String], baseDir: Path): Outcome =
    CliParser
      .parse(args)
      .flatMap(analyze(_, baseDir))
      .fold(message => Outcome("", message + "\n", ErrorCode), identity)

  private def analyze(config: CliConfig, baseDir: Path): Either[String, Outcome] =
    for
      tasty <- TastyLoader.load(Path.of(config.tastyDir))
      data <- CoverageLoader.load(Path.of(config.coverageDir), Some(baseDir))
    yield
      val rows = Analyzer.rows(tasty, data, baseDir)
      val csv = CsvWriter.render(UnitRow.select(rows, config.threshold, config.all))
      Outcome(csv, "", if UnitRow.exceeds(rows, config.threshold) then AboveThresholdCode else 0)
