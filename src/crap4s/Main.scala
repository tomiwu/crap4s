package crap4s

import crap4s.tasty.TastyLoader

import java.nio.file.Path

@main def crap(args: String*): Unit =
  CliParser.parse(args.toList) match
    case Left(message) =>
      Console.err.println(message)
      sys.exit(1)
    case Right(config) =>
      runWithConfig(config)

private def runWithConfig(config: CliConfig): Unit =
  TastyLoader.load(Path.of(config.tastyDir)) match
    case Left(message) =>
      Console.err.println(message)
      sys.exit(1)
    case Right(_) =>
      if Crap4sDebug.enabled then println(config.debugLine)
      sys.exit(0)

private object Crap4sDebug:
  def enabled: Boolean =
    Option(System.getenv("CRAP4S_DEBUG"))
      .exists(v => v != "0" && !v.equalsIgnoreCase("false"))
