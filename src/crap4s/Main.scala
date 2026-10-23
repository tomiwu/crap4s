package crap4s

import crap4s.output.colourEnabled

import java.nio.file.Path

@main def crap(args: String*): Unit =
  val color = colourEnabled(
    name => Option(System.getenv(name)),
    System.console() != null
  )
  val outcome = App.run(args.toList, Path.of("").toAbsolutePath.toString, color)
  print(outcome.out)
  Console.err.print(outcome.err)
  sys.exit(outcome.code.value)
