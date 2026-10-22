# crap4s coding standards

Scope: all Scala code in this repository. Scala 3 only, Scala CLI build.
Each rule has one reason. Source tags:
- **[A8]**: from "Scala Architecture and Design Patterns - Part 8", Jonathan A., LinkedIn, 2026-10-17
  (https://www.linkedin.com/pulse/scala-architecture-design-patterns-part-8-jonathan-altamura-rxsxc/).
- **[S3]**: general Scala 3 practice.

## 1. Design

1. **Keep each concern in its own object or file** (TASTy loading, coverage parsing, complexity, mapping, CRAP formula, report output, CLI). [A8] Reason: a change then touches the smallest part of the code.
2. **Compose the pipeline from small steps** in `Main`: read, analyze, map, score, write. [A8] Reason: each step is easy to read and to test alone.
3. **Model the domain with case classes and enums** (`Unit`, `Coverage`, `Score`), not with raw `String`, `Int` or tuples. [A8] Reason: names in code show the domain, and the compiler finds mix-ups.
4. **Use a trait only where there are two or more real implementations, or a test double is necessary** (for example, a coverage source). [A8] Reason: abstraction has a cost; do not add it without a need.
5. **Inject dependencies through constructor or function parameters.** Do not use a DI framework. [A8] Reason: plain parameters are enough and keep tests simple.
6. **Keep side effects at the edges.** Only the CLI layer reads files, prints, and calls `sys.exit`. Core logic is pure functions. [A8] Reason: pure code is predictable and needs no files to test.
7. **Write a unit test for each business rule** (each complexity rule, each formula case) without file system access where possible. [A8] Reason: small tests find the exact failure.

## 2. Scala 3 code

8. **Use Scala 3 syntax**: `enum`, `extension`, `given`/`using`, top-level definitions, `@main`. [S3] Reason: one style, no Scala 2 forms.
9. **Use braceless (indentation) syntax** and `end` markers only for definitions longer than 20 lines. [S3] Reason: same style everywhere; scalafmt enforces it.
10. **Give an explicit result type to every public or top-level definition.** [S3] Reason: stable API and faster, clearer compiler errors.
11. **Do not use `null`, `return`, `var` in core logic, or `asInstanceOf`.** If a Java API can return `null`, wrap it in `Option(...)` at once. [S3] Reason: these break the pure, type-safe core.
12. **Report errors as values** (`Either[Error, A]` with an `enum Error`). Throw only in the CLI layer, and only to exit. [S3] Reason: errors stay visible in types.
13. **Use immutable collections and `Vector`/`List`.** [S3] Reason: no shared mutable state.
14. **Keep the CRAP score of every unit below 4**: small methods, few branches, high coverage. [crap4s project rule] Reason: the tool must pass its own gate.
15. **Write modules as capabilities (opaque function types) and use them with `?=>`.** Wire them only in `App.scala`. Put the flow only in `Pipeline.scala`. Only `Main` and the input adapters touch files or the console. [S3] Reason: the flow has no wiring and no side effects, so a test can give it fake modules.

    ```scala
    // Capabilities.scala: a module is an opaque function type with the function type as upper bound
    opaque type Units <: () => Either[Error, List[CodeUnit]] = () => Either[Error, List[CodeUnit]]
    object Units:
      def apply(f: () => Either[Error, List[CodeUnit]]): Units = f
    def mapping(using m: Mapping): Mapping = m

    // Pipeline.scala: the flow is one flatMap chain and asks for the modules with ?=>
    def pipeline: (Units, Coverage, Mapping, Scoring, Report) ?=> Either[Error, Result] =
      loadInputs
        .flatMap(mapping)
        .flatMap(scoring)
        .flatMap(report)

    // App.scala: the only place that picks the implementations
    given Units = tastyUnitsAt(config.tastyDirs, entries, baseDir)
    given Report = reportFor(config.format, config.threshold, config.all, color)
    pipeline
    ```

## 3. Formatting (scalafmt)

Proposed `.scalafmt.conf` in the repository root (scalafmt 3.11.5 is the newest release on Maven Central on 2026-10-20):

```hocon
version = "3.11.5"
runner.dialect = scala3
maxColumn = 100
align.preset = none
rewrite.scala3.convertToNewSyntax = true
rewrite.scala3.removeOptionalBraces = true
rewrite.scala3.insertEndMarkerMinLines = 20
rewrite.rules = [RedundantBraces, RedundantParens, SortModifiers, Imports]
rewrite.imports.sort = scalastyle
newlines.source = keep
docstrings.style = Asterisk
fileOverride { "glob:**/*.sc" { runner.dialect = scala3 } }
```

Check: `scala-cli fmt --check .` Fix: `scala-cli fmt .`

## 4. Static checks

### 4.1 Compiler options (Scala 3.10)

Put these in `project.scala`:

```scala
//> using options -deprecation -feature -unchecked -Werror
//> using options -Wunused:all -Wvalue-discard -Wnonunit-statement -Wsafe-init
//> using options -Wconf:any:verbose
```

Reasons: `-Werror` makes each warning a CI failure; `-Wunused:all` finds dead code and is necessary for the Scalafix `RemoveUnused` rule; `-Wvalue-discard` and `-Wnonunit-statement` find lost results; `-Wsafe-init` finds bad initialization order.
Optional, later: `-Yexplicit-nulls` (strict, but tasty-query and Java APIs make it noisy).

### 4.2 Scalafix (through Scala CLI)

`scala-cli --power fix --check .` runs Scalafix (default version 0.14.9 in Scala CLI 1.18.0) and fails when a rewrite is necessary. Proposed `.scalafix.conf`:

```hocon
rules = [
  RemoveUnused,
  DisableSyntax,
  LeakingImplicitClassVal,
  NoValInForComprehension,
  RedundantSyntax,
  OrganizeImports
]
DisableSyntax {
  noNulls = true
  noReturns = true
  noAsInstanceOf = true
  noIsInstanceOf = true
  noXml = true
  noFinalize = true
}
OrganizeImports {
  targetDialect = Scala3
  removeUnused = true
}
```

Note: `RemoveUnused` and `OrganizeImports.removeUnused` need `-Wunused:all`.

The local rule `PassThrough` (`scalafix/PassThrough.scala`, enabled in `.scalafix.conf`) flags a `def` whose body only calls another function with its own parameters or constants. Inline such a def. The rule skips overrides, defs in a type with parents, a companion `apply` and defs with `@nowarn("crap4s:passthrough")`. The `lint` job fails on 1 or more findings. Scalafix local rules are Scala 2.13 code, so `scalafix/` is its own Scala CLI project with its own tests (`scala-cli test scalafix`). The main build excludes it, and scalafmt skips it.

Three more local rules guard the capability style. They are in `scalafix/` and use the names in `CapabilityStyle` in `.scalafix.conf`.

- `NoExtendsCapability`: no class, object or enum case extends a capability.
- `CapabilityObject`: no `object` holds only defs. The companion of an opaque type and `App` are allowed.
- `GivenOnlyInApp`: a `given` of a capability is only in `App.scala`. Test sources are allowed.

When you add a capability, add its name to `CapabilityStyle`.

### 4.3 WartRemover (optional)

WartRemover has a build for Scala 3.10.0 (`org.wartremover:wartremover_3.10.0:3.6.3` on Maven Central, checked 2026-10-20). Use it only if Scalafix `DisableSyntax` is not enough:

```scala
//> using plugin org.wartremover:::wartremover:3.6.3
//> using options -P:wartremover:traverser:org.wartremover.warts.Unsafe
```

Risk: the plugin version must match the exact Scala version, so each Scala update can block the build until a new release.

## 5. CI gate

On each pull request, GitHub Actions runs: format check, compile with `-Werror`, Scalafix check, tests, and the crap4s self-check (CRAP < 4 for every unit). The Sandcastle loop merges a PR only when all checks pass.
