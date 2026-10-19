package crap4s

import java.nio.file.Path

@main def crap(args: String*): Unit =
  val outcome = Cli.run(args.toList, Path.of("").toAbsolutePath)
  print(outcome.out)
  Console.err.print(outcome.err)
  sys.exit(outcome.code)
