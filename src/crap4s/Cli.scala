package crap4s

import crap4s.coverage.ScoverageSource
import crap4s.tasty.TastySource

import java.nio.file.Files
import java.nio.file.Path

final case class Outcome(out: String, err: String, code: Int)

object Cli:
  val AboveThresholdCode: Int = 1
  val ErrorCode: Int = 2

  def run(args: List[String], baseDir: Path): Outcome =
    if args.contains("--help") then Outcome(Usage.text, "", 0) else analyzeArgs(args, baseDir)

  private def analyzeArgs(args: List[String], baseDir: Path): Outcome =
    CliParser
      .parse(args)
      .flatMap(analyze(_, baseDir))
      .fold(failure, identity)

  def failure(error: Error): Outcome = Outcome("", error.message + "\n", ErrorCode)

  private def analyze(config: CliConfig, baseDir: Path): Either[Error, Outcome] =
    val (entries, missing) = config.classpath.map(Path.of(_)).partition(Files.exists(_))
    val warnings = missing.map(entry => s"Warning: classpath entry does not exist: $entry\n")
    val tasty = TastySource(config.tastyDirs.map(Path.of(_)), entries, baseDir)
    val coverage = ScoverageSource(config.coverageDirs.map(Path.of(_)), baseDir)
    for
      writer <- Registry.writer(config.format)
      rows <- Analyzer.rows(tasty, coverage)
    yield
      val text = writer.render(rows, config.threshold, config.all)
      val code = if UnitRow.exceeds(rows, config.threshold) then AboveThresholdCode else 0
      Outcome(text, warnings.mkString, code)
end Cli
