package crap4s

import crap4s.output.colourEnabled

import java.nio.file.Path

@main def crap(args: String*): Unit =
  val color = colourEnabled(
    name => Option(System.getenv(name)),
    Option(System.console()).exists(_.isTerminal)
  )
  val outcome = App.run(args.toList, Path.of("").toAbsolutePath.toString, color)
  print(outcome.out)
  Console.err.print(outcome.err)
  sys.exit(outcome.code.value)
