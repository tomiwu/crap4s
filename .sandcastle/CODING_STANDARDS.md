# Coding Standards

<!-- Customize this file with your project's coding standards.
     The reviewer agent loads it during code review via @.sandcastle/CODING_STANDARDS.md
     so these standards are enforced during review without costing tokens during implementation. -->

## Style
- Scala 3 (significant indentation OK), immutable data, ADTs via enum/case class.
- No `null`, no `asInstanceOf` without justification; prefer `Either`/`Option`.
## Errors and side effects
- No exception may escape. Every failure is a typed `Error` case in `Either`. No `throw`, no unwrapped `try`, no `.get`, `.head`, `.last`, `.tail`, `.reduce`, `.toInt`, `.toDouble`, `require`, `assert` or `???`.
- Side effects (exit, print, console, env, `java.io`, `Files`, `var`, mutable collections) only in capability implementations at the edge. Core and pipeline are pure.
- One last-resort guard in `Main` only: exit 2, one line, no stack trace.
- Scalafix rules `NoThrow`, `NoUncheckedTry`, `NoUnsafeCall` and `SideEffectsAtEdge` enforce this. Allowed edge files are in `.scalafix.conf` (`UncheckedTryFiles`, `UncheckedTryGuards`, `SideEffectsAllow`). See `docs/CODING_STANDARDS.md` rules 20 to 23.
## Testing
- munit via `scala-cli test .`; every unit-counting rule from docs/PRD.md has a test.
## Architecture
- Small pure modules: tasty loading, complexity, coverage parsing, mapping, CRAP/CSV output.
