package crap4s.edge

import crap4s.Env
import crap4s.EnvOps
import crap4s.Terminal
import crap4s.TerminalOps

import java.nio.file.Path

/** The terminal of the JVM process. Only this file touches the console. */
val systemTerminal: Terminal = Terminal(
  TerminalOps(
    () => Option(System.console()).isDefined,
    text => print(text),
    text => Console.err.print(text)
  )
)

/** The environment of the JVM process. */
val systemEnv: Env = Env(
  EnvOps(
    name => Option(System.getenv(name)),
    name => Option(System.getProperty(name)),
    () => Path.of("").toAbsolutePath.toString
  )
)
