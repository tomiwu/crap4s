# Classpath for other projects

The TASTy files of a project refer to its dependencies. crap4s must find these dependencies.
Give them with `--classpath` or `--classpath-file`.

## Options

- `--classpath <path:path:...>`: entries separated by `:`. You can use the option more than once.
- `--classpath-file <file>`: a file with a classpath. The file has one line with entries
  separated by `:`, or one entry on each line. Empty lines are ignored.
- `--tasty <dir>` and `--coverage <dir>` can occur more than once for multi-module projects.
  crap4s analyzes the units of all TASTy roots. It merges the coverage data of all directories.

If a classpath entry does not exist, crap4s writes a warning to stderr and continues.
If a symbol does not resolve, crap4s stops with an error that tells you to add the classpath.

## How to get the classpath

### sbt

```
sbt "export Test/fullClasspath"
sbt "export Runtime/fullClasspath"
```

The last line of the output is the classpath. Save it in a file and use `--classpath-file`.

### Mill

```
mill show <module>.runClasspath
mill show <module>.test.runClasspath
```

### Scala CLI

```
scala-cli compile . --print-class-path
scala-cli compile . --test --print-class-path
```

## Example

```
sbt --error "export Test/fullClasspath" > classpath.txt
crap4s --tasty target/scala-3.10.0/classes --coverage target/scala-3.10.0/coverage \
  --classpath-file classpath.txt
```
