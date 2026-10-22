package crap4s

import crap4s.coverage.CoverageLoader
import crap4s.tasty.TastyLoader
import tastyquery.Exceptions.InvalidProgramStructureException

import java.nio.file.Files
import java.nio.file.Path

final case class Outcome(out: String, err: String, code: Int)

object Cli:
  val AboveThresholdCode: Int = 2
  val ErrorCode: Int = 1

  def run(args: List[String], baseDir: Path): Outcome =
    if args.contains("--help") then Outcome(Usage.text, "", 0) else analyzeArgs(args, baseDir)

  private def analyzeArgs(args: List[String], baseDir: Path): Outcome =
    CliParser
      .parse(args)
      .flatMap(analyze(_, baseDir))
      .fold(error => Outcome("", error.message + "\n", ErrorCode), identity)

  private def analyze(config: CliConfig, baseDir: Path): Either[Error, Outcome] =
    val (entries, missing) = config.classpath.map(Path.of(_)).partition(Files.exists(_))
    val warnings = missing.map(entry => s"Warning: classpath entry does not exist: $entry\n")
    for
      tasty <- guard(TastyLoader.load(config.tastyDirs.map(Path.of(_)), entries))
      data <- CoverageLoader.loadAll(config.coverageDirs.map(Path.of(_)), Some(baseDir))
      rows <- guard(Analyzer.rows(tasty, data, baseDir))
    yield
      val csv = CsvWriter.render(UnitRow.select(rows, config.threshold, config.all))
      val code = if UnitRow.exceeds(rows, config.threshold) then AboveThresholdCode else 0
      Outcome(csv, warnings.mkString, code)

  private def guard[A](step: => Either[Error, A]): Either[Error, A] =
    try step
    catch case e: InvalidProgramStructureException => Left(Error.UnresolvedSymbol(e.getMessage))
end Cli
