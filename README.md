# crap4s

crap4s calculates the CRAP score (Change Risk Anti-Patterns, by Robert C. Martin) for Scala 3 code. It reads TASTy files with tasty-query. It reads coverage data from the Scala 3 compiler option `-coverage-out`. Refer to `docs/PRD.md` for the specification.

## Install

Java 25 or later must be on the `PATH`. Download the latest release to a directory on the `PATH`:

```sh
mkdir -p ~/.local/bin
curl -fL -o ~/.local/bin/crap4s https://github.com/tomiwu/crap4s/releases/latest/download/crap4s
chmod +x ~/.local/bin/crap4s
crap4s --help
```

You can also run the file with `java -jar crap4s`. Refer to `docs/release.md` for the release process.

## Usage

crap4s needs three inputs: the TASTy files of your project, the `-coverage-out` data, and the classpath of the dependencies. Do these steps with every build tool:

1. Enable the Scala 3 compiler option `-coverage-out:<dir>` for the main code. The compiler writes the instrumented classes and `scoverage.coverage`.
2. Run the tests. The tests write `scoverage.measurements.*` to the same directory.
3. Get the classpath and save it in a file.
4. Run `crap4s`.

Use an absolute path or a path relative to the project root for `<dir>`. Do not compile again after the tests. A new compile with `-coverage-out` resets the directory. The exit code is 0 when no unit is above the threshold. It is 2 when a unit is above the threshold. Refer to `docs/classpath.md` for the classpath options.

The examples use a threshold of 6. Use `--all` to show all units.

### sbt

1. Enable `-coverage-out` in `build.sbt`. Use `scalacOptions`, not `Test / scalacOptions`. The main code must be instrumented too.

   ```scala
   scalacOptions += "-coverage-out:target/coverage"
   ```

   You can also set the option in the command line: `sbt 'set scalacOptions += "-coverage-out:target/coverage"' test`.
2. Run the tests: `sbt clean test`.
3. Get the classpath. The last line of the output is the classpath.

   ```sh
   sbt --error "export Test/fullClasspath" | tail -1 > target/classpath.txt
   ```

4. Run crap4s.

   ```sh
   crap4s --tasty target/scala-3.10.0/classes --coverage target/coverage \
     --classpath-file target/classpath.txt --threshold 6
   ```

| Output | Path |
| --- | --- |
| TASTy files (main) | `target/scala-<scala version>/classes` |
| TASTy files (tests) | `target/scala-<scala version>/test-classes` |
| Coverage data | the directory in `-coverage-out`, for example `target/coverage` |

For a multi-module build, repeat `--tasty` and `--coverage` for each module. Use one `-coverage-out` directory for each module, or one shared directory.

### Mill

1. Enable `-coverage-out` in the `scalacOptions` of the module. The path must be absolute, because Mill runs the compiler and the tests in a sandbox directory. In `build.mill`:

   ```scala
   object a extends ScalaModule {
     def scalaVersion = "3.10.0"
     def scalacOptions = Task {
       Seq(
         s"-coverage-out:${mill.api.BuildCtx.workspaceRoot}/out/crap-coverage",
         s"-sourceroot:${mill.api.BuildCtx.workspaceRoot}"
       )
     }
     object test extends ScalaTests { /* test framework */ }
   }
   ```

2. Run the tests: `./mill a.test`.
3. Get the classpath. `mill show` prints a JSON array. Some entries have a `ref:` prefix. Remove the prefix.

   ```sh
   ./mill --ticker false show a.test.runClasspath | jq -r '.[]' \
     | sed -E 's/^q?ref:v[0-9]+:[0-9a-f]+://' | paste -sd: - > out/classpath.txt
   ```

   crap4s writes a warning for each entry that does not exist (for example an empty `resources` directory). You can ignore these warnings.
4. Run crap4s.

   ```sh
   crap4s --tasty out/a/compile.dest/classes --coverage out/crap-coverage \
     --classpath-file out/classpath.txt --threshold 6
   ```

| Output | Path |
| --- | --- |
| TASTy files (main) | `out/<module>/compile.dest/classes` |
| TASTy files (tests) | `out/<module>/test/compile.dest/classes` |
| Coverage data | the directory in `-coverage-out`, for example `out/crap-coverage` |

### Scala CLI

1. Compile with `-O -coverage-out:<dir>`. Write the classes to a directory with `-d`, because Scala CLI keeps them in a directory with a hash in the name.

   ```sh
   opts=(-O "-coverage-out:out/coverage" -O "-sourceroot:$PWD")
   scala-cli compile . "${opts[@]}" -d out/classes
   ```

2. Get the classpath. Do this before the tests.

   ```sh
   scala-cli compile . "${opts[@]}" --print-class-path > out/classpath.txt
   ```

3. Run the tests. Use the same options.

   ```sh
   scala-cli test . "${opts[@]}"
   ```

4. Run crap4s.

   ```sh
   crap4s --tasty out/classes --coverage out/coverage \
     --classpath-file out/classpath.txt --threshold 6
   ```

| Output | Path |
| --- | --- |
| TASTy files (main) | `.scala-build/<project>_<hash>/classes/main`, or the directory in `-d` (for example `out/classes`) |
| TASTy files (tests) | `.scala-build/<project>_<hash>/classes/test` |
| Coverage data | the directory in `-coverage-out`, for example `out/coverage` |

### GitHub Actions

`docs/github-actions-example.yml` is a complete workflow for a Scala CLI project. Copy it to `.github/workflows/crap.yml` in your repository. It installs crap4s from the latest release. The job fails if a unit is above the threshold. For sbt and Mill, replace the build steps with the steps from the sections above.

This repository also has a composite action (`action.yml`). It installs crap4s from a release and runs it. Use it after your own build steps:

```yaml
- uses: tomiwu/crap4s@main
  with:
    tasty: target/scala-3.10.0/classes
    coverage: target/coverage
    classpath-file: target/classpath.txt
    threshold: "6"
    version: latest
```

| Input | Description | Default |
| --- | --- | --- |
| `tasty` | TASTy directories, separated by a space | required |
| `coverage` | `-coverage-out` directories, separated by a space | required |
| `classpath-file` | File with the classpath | none |
| `threshold` | CRAP threshold | `6` |
| `version` | Release tag, for example `v0.1.0`, or `latest` | `latest` |

We selected a composite action and not a reusable workflow. A reusable workflow (`workflow_call`) runs as a complete job. It cannot know how your build compiles the code, runs the tests and gets the classpath. Each build tool needs other steps. A composite action is one step. You put it after your own build steps, so it works with sbt, Mill and Scala CLI. Pin `version` to a release tag for stable builds.

## Development loop (Sandcastle)

Sandcastle runs an AI agent in a Podman container. The agent does one GitHub issue (label `Sandcastle`) in each iteration. The loop does these steps for each issue:

1. Select an open issue that has no open "Blocked by #N" issue. Select the highest priority first. If two issues have the same priority, select the lower number.
2. Implement the issue on a new branch. Then review the change.
3. Run `scala-cli test .`. If `scripts/self-crap.sh` exists, run it. Each unit must have a CRAP score less than 4.
4. If a check fails, let the agent fix the code (3 attempts maximum). If the checks still fail, stop the loop.
5. Open a PR. Wait until all GitHub Actions checks pass. Then squash-merge the PR and close the issue. If no check starts or a check fails, do not merge. Stop the loop.

### Priority

Use one of these labels: `priority: critical`, `priority: high`, `priority: medium`, `priority: low`. An issue without a priority label is medium. The loop reads the labels before each iteration. The loop does not stop an issue that is in progress.

To set the priority of issue N:

```sh
gh issue edit N --add-label 'priority: high'
```

To change the priority, also remove the old label:

```sh
gh issue edit N --remove-label 'priority: medium' --add-label 'priority: high'
```

### Prerequisites

- Node.js 20 or later, Podman, and the GitHub CLI (`gh`).
- Run `npm install`.
- Write the keys in `.sandcastle/.env` (see `.sandcastle/.env.example`). Git ignores this file. Do not commit it.
- Build the image one time: `npx @ai-hero/sandcastle podman build-image`.

### Run the loop with Claude Code (default, Claude Sonnet 5.5)

Claude Code writes and reviews all code by default.

```sh
MAX_ITERATIONS=10 npx tsx .sandcastle/main.mts
```

### Run the loop with Cursor (option, Composer 2.5 Fast)

```sh
AGENT=cursor MAX_ITERATIONS=10 npx tsx .sandcastle/main.mts
```

For Claude Code, set `CLAUDE_CODE_OAUTH_TOKEN` (run `claude setup-token`) or `ANTHROPIC_API_KEY` in `.sandcastle/.env`.

If Cursor has no more credits during the loop, the loop changes to Claude Code automatically. Use `CURSOR_MODEL` or `CLAUDE_MODEL` to change the model. Use `SKIP_ISSUES=17,18` to skip issues.

## End-to-end test of the release artifact

`scripts/e2e-release.sh` builds the artifact. It compiles and tests `samples/with-dependency/` with `-coverage-out`. Then it runs crap4s with `--classpath-file` and checks the CSV. Pass an artifact path to test a downloaded release. The workflow `E2E release` runs the script on `workflow_dispatch` and on tags. It is not a required check.
