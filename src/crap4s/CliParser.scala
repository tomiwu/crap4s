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

  private type Step = Either[String, (List[String], Flags)]

  private val valueOptions: Map[String, (Flags, String) => Either[String, Flags]] = Map(
    "--tasty" -> ((flags, value) => Right(flags.copy(tasty = Some(value)))),
    "--coverage" -> ((flags, value) => Right(flags.copy(coverage = Some(value)))),
    "--threshold" ->
      ((flags, value) =>
        parseThreshold(value).map(threshold => flags.copy(threshold = threshold))
      )
  )

  private def parseLoop(rest: List[String], flags: Flags): Either[String, Flags] =
    rest match
      case Nil => Right(flags)
      case head :: tail =>
        step(head, tail, flags).flatMap((remaining, next) => parseLoop(remaining, next))

  private def step(head: String, tail: List[String], flags: Flags): Step =
    (head, tail) match
      case ("--all", _) => Right((tail, flags.copy(all = true)))
      case (option, value :: remaining) =>
        valueOptions.get(option).fold(unknown(option))(_(flags, value).map((remaining, _)))
      case _ => unknown(head)

  private def unknown(option: String): Step = Left(s"Unknown option: $option")

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
end CliParser
