package crap4s

import java.nio.file.Files
import java.nio.file.Path

object CliParser:

  def parse(args: List[String]): Either[String, CliConfig] =
    readFlags(args).flatMap(validateRequired).flatMap(validateDirectories)

  private case class Flags(
      tasty: Option[String],
      coverage: Option[String],
      threshold: Double,
      all: Boolean
  )

  private def readFlags(args: List[String]): Either[String, Flags] =
    parseLoop(args, Flags(None, None, 6.0, false))

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

  private def validateRequired(flags: Flags): Either[String, (String, String, Double, Boolean)] =
    (flags.tasty, flags.coverage) match
      case (Some(t), Some(c)) => Right((t, c, flags.threshold, flags.all))
      case (None, _)          => Left("Missing required option: --tasty")
      case (_, None)          => Left("Missing required option: --coverage")

  private def validateDirectories(
      data: (String, String, Double, Boolean)
  ): Either[String, CliConfig] =
    val (tasty, coverage, threshold, all) = data
    if !isExistingDirectory(tasty) then Left(s"Directory does not exist: $tasty")
    else if !isExistingDirectory(coverage) then Left(s"Directory does not exist: $coverage")
    else Right(CliConfig(tasty, coverage, threshold, all))

  private def isExistingDirectory(path: String): Boolean =
    val p = Path.of(path)
    Files.isDirectory(p)
