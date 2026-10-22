package crap4s

import crap4s.cli.Cli
import crap4s.output.Colour

import java.nio.file.Path

@main def crap(args: String*): Unit =
  val color = Colour.enabled(
    name => Option(System.getenv(name)),
    Option(System.console()).exists(_.isTerminal)
  )
  val outcome = Cli.run(args.toList, Path.of("").toAbsolutePath, color)
  print(outcome.out)
  Console.err.print(outcome.err)
  sys.exit(outcome.code.value)
