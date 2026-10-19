package crap4s

import java.nio.file.Files
import java.nio.file.Path

object CliParser:

  def parse(args: List[String]): Either[String, CliConfig] =
    readFlags(args).flatMap(toConfig)

  private case class Flags(
      tasty: Option[String],
      coverage: Option[String],
      threshold: Double,
      all: Boolean
  )

  private def readFlags(args: List[String]): Either[String, Flags] =
    parseLoop(args, Flags(None, None, CliConfig.DefaultThreshold, false))

  private def parseLoop(rest: List[String], flags: Flags): Either[String, Flags] =
    rest match
      case Nil => Right(flags)
      case "--tasty" :: value :: tail =>
        parseLoop(tail, flags.copy(tasty = Some(value)))
      case "--coverage" :: value :: tail =>
        parseLoop(tail, flags.copy(coverage = Some(value)))
      case "--threshold" :: value :: tail =>
        parseThreshold(value).flatMap(t => parseLoop(tail, flags.copy(threshold = t)))
      case "--all" :: tail =>
        parseLoop(tail, flags.copy(all = true))
      case unknown :: _ =>
        Left(s"Unknown option: $unknown")

  private def parseThreshold(value: String): Either[String, Double] =
    value.toDoubleOption.toRight(s"Invalid threshold: $value")

  private def toConfig(flags: Flags): Either[String, CliConfig] =
    for
      tasty <- flags.tasty.toRight("Missing required option: --tasty")
      coverage <- flags.coverage.toRight("Missing required option: --coverage")
      _ <- requireDirectory(tasty)
      _ <- requireDirectory(coverage)
    yield CliConfig(tasty, coverage, flags.threshold, flags.all)

  private def requireDirectory(path: String): Either[String, Unit] =
    if Files.isDirectory(Path.of(path)) then Right(())
    else Left(s"Directory does not exist: $path")
