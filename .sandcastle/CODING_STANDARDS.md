# Coding Standards

<!-- Customize this file with your project's coding standards.
     The reviewer agent loads it during code review via @.sandcastle/CODING_STANDARDS.md
     so these standards are enforced during review without costing tokens during implementation. -->

## Style
- Scala 3 (significant indentation OK), immutable data, ADTs via enum/case class.
- No `null`, no `asInstanceOf` without justification; prefer `Either`/`Option`.
## Testing
- munit via `scala-cli test .`; every unit-counting rule from docs/PRD.md has a test.
## Architecture
- Small pure modules: tasty loading, complexity, coverage parsing, mapping, CRAP/CSV output.
