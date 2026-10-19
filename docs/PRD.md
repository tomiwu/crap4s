# PRD: crap4s — CRAP score for Scala 3

## 1. Goal
crap4s is an open source tool. It is a CLI that runs on Scala CLI. The tool analyzes a Scala 3 project. It calculates the cyclomatic complexity (CC), the test coverage, and the Uncle Bob CRAP score. Then it shows the methods and lambdas that have a high risk.

## 2. Users
- A Scala developer who finds high-risk methods on a local computer.
- A CI pipeline that uses the tool as a quality gate. If a unit is above the threshold, the exit code is not 0.

## 3. Scope
**In scope:**
- A standalone CLI. Scala CLI runs it.
- Scala 3 only. The tool analyzes compiled TASTy with tasty-query (`ch.epfl.scala::tasty-query:1.10.0`).
- Coverage comes from the built-in Scala 3 compiler option `-coverage-out`.
- The output is CSV.

**Out of scope (non-goals):**
- Scala 2.13.
- Custom code instrumentation and JaCoCo.
- sbt/Mill plugins (later, M4).
- HTML (later, M4).
- License: later.

## 4. Functional requirements
1. Load the `.tasty` files from the given directory with tasty-query. Use the full trees, the `SourcePosition` offsets, and the `isSynthetic` and `isInline` flags.
2. Build the classpath explicitly. Include the JRE (`jrt:/`).
3. Load `scoverage.coverage` and `scoverage.measurements.*` from the `-coverage-out` directory.
4. Divide the code into units. Calculate CC, coverage, and CRAP for each unit.
5. Calculate aggregates for each file and for each package.
6. Write CSV. Sort the rows by `crap` in descending order.
7. If a unit is above the threshold, exit with a code that is not 0.

## 5. CLI specification
```
crap --tasty out/classes --coverage out/coverage --threshold 6 [--all]
```
| Option | Description | Default |
|---|---|---|
| `--tasty` | directory with `.tasty` files | required |
| `--coverage` | `-coverage-out` directory | required |
| `--threshold` | CRAP threshold | 6 |
| `--all` | show all units, not only the units above the threshold | off |

Exit codes: `0` if no unit is above the threshold. Not 0 if a unit is above the threshold. Detailed error codes: *TBD*. Syntax for the classpath: *TBD*.

## 6. Algorithm
### 6.1 Units
- A unit is a method (`def`) or a top-level lambda in a method body.
- Nested lambdas and local `def`s are part of the parent.
- Lambdas with CC = 1 are part of the parent.

### 6.2 Cyclomatic complexity (start = 1)
Add +1 for each of these items:
- `if`
- each `case` in `match` and `catch`, except the first
- `while`
- `&&` and `||` (identify them by symbol, because they are usual method calls)
- a guard in `case` and in `for` (in TASTy, a guard in `for` is already `withFilter`). A `for` without guards adds nothing.

Ignore synthetic code (`isSynthetic`). Do not count `inline` expansions (`Inlined`) at the call site. Count only the `inline def` definition.

### 6.3 Coverage
- Assign the statements from `scoverage.coverage` to units by the source offset ranges `[start, end)`.
- A statement is executed if its id is in `scoverage.measurements.*`.
- `cov` = executed statements / all statements of the unit.
- Before you write the parser, examine the exact file format in `scala3/compiler/src/dotty/tools/dotc/coverage/Serializer.scala`.
- A unit without statements in the report: *TBD*.

### 6.4 CRAP
`CRAP = cc² × (1 − cov)³ + cc`. Default threshold: 6.

### 6.5 Aggregates
For each file and for each package. Aggregate metric (max, sum, average): *TBD*.

## 7. Output
CSV with this header:
```
file,line,unit,cc,cov,crap
```
- Sort the rows by `crap` in descending order.
- By default, show only the units above the threshold. With `--all`, show all units.
- Format of the `unit` name (for example, for lambdas) and number precision: *TBD*.
- Where and how to write the aggregates: *TBD*.

## 8. Milestones
- **M1:** load TASTy, divide into units, CC, and CSV (no coverage).
- **M2:** `-coverage-out` parser, offset mapping, `cov`, and CRAP.
- **M3:** CI gate (threshold, exit code, `--all`) and aggregates for each file and package.
- **M4:** HTML report, sbt/Mill integrations.

## 9. Risks
- Positions of synthetic trees and `Inlined` trees can be different from the positions in the coverage report.
- You must build the classpath manually, including the JRE (`jrt:/`).
- Heuristics: `&&`/`||` are method calls, and guards in `for` are expanded to `withFilter`.
- Dependency on the TASTy format version: a new Scala version can make a tasty-query update necessary.

## 10. Process
Record each iteration in the repository. Process tool: *TBD*.

## 11. Open questions (TBD)
- License.
- Tool name and distribution method (script, native image, Coursier).
- How to supply the classpath of the project dependencies.
- Aggregate metric and its location in the output.
- How to process units without coverage data.
- Format of the `unit` column and number precision.
- Exclude test code from the analysis or not.
- Tool to track iterations in the repository.
