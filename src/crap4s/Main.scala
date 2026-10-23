package crap4s

import crap4s.edge.systemEnv
import crap4s.edge.systemTerminal

@main def crap(args: String*): Unit =
  sys.exit(App.main(args.toList)(using systemTerminal, systemEnv))
