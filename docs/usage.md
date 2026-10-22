# Usage guide

This guide shows how to run crap4s with sbt, Mill and Scala CLI, and in GitHub Actions. Refer to the [README](../README.md) for the flags and the exit codes.

## Overview

crap4s needs three inputs: the TASTy files of your project, the `-coverage-out` data, and the classpath of the dependencies. Do these steps with every build tool:

1. Enable the Scala 3 compiler option `-coverage-out:<dir>` for the main code. The compiler writes the instrumented classes and `scoverage.coverage`.
2. Run the tests. The tests write `scoverage.measurements.*` to the same directory.
3. Get the classpath and save it in a file.
4. Run `crap4s`.

Use an absolute path or a path relative to the project root for `<dir>`. Do not compile again after the tests. A new compile with `-coverage-out` resets the directory. The exit code is 0 when no unit is above the threshold. It is 1 when a unit is above the threshold. It is 2 for bad input or an error: the message goes to stderr and stdout stays empty. Refer to [classpath.md](classpath.md) for the classpath options.

The examples use a threshold of 6. The default output format is `agent`. It prints one line for each unit above the threshold, worst first: `path:line unit cc=5 cov=0.20 crap=17.80`. Then it prints one summary line: `FAIL 3/100 max=17.80`. When all units pass, it prints only `PASS 100 max=3.10`. Use `--all` to list all units before the summary line.

Use `--format human` for an aligned table of all units, worst first, with a summary line. It shows units above the threshold in red, unless `NO_COLOR` is set or stdout is not a TTY.

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


## More guides

- You can also run the release file with `java -jar crap4s`. Refer to [release.md](release.md) for the release process.
- [classpath.md](classpath.md) explains the classpath options.
- [coverage-format.md](coverage-format.md) describes the coverage data.
- [development.md](development.md) describes the development loop.
