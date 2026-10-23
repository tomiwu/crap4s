package crap4s

/** The operations of a terminal: the TTY check, stdout and stderr. */
final case class TerminalOps(isTty: () => Boolean, out: String => Unit, err: String => Unit)

/** Capability: the terminal of the process. */
opaque type Terminal <: TerminalOps = TerminalOps

object Terminal:
  def apply(ops: TerminalOps): Terminal = ops

/** The operations on the environment: variables, system properties and the working directory. */
final case class EnvOps(
    variable: String => Option[String],
    property: String => Option[String],
    workingDir: () => String
)

/** Capability: the environment of the process. */
opaque type Env <: EnvOps = EnvOps

object Env:
  def apply(ops: EnvOps): Env = ops

/** The single line for an exception that nobody handled. */
def internalError(t: Throwable): String =
  s"crap4s: internal error: ${t.getClass.getName}: ${Option(t.getMessage).getOrElse("")}\n"

/** The last-resort guard: any Throwable gives one line on stderr and the exit code 2. */
def guarded(body: => Int)(using terminal: Terminal): Int =
  try body
  catch
    case t: Throwable =>
      terminal.err(internalError(t))
      2
