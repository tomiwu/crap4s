# Release

## Options compared

| Option | Result |
| --- | --- |
| `scala-cli --power package --assembly` | Works. It makes one executable file (a bash/bat preamble plus a JAR). It needs only a JVM 25 or later. A GitHub Release can host the file. **Selected.** |
| `scala-cli --power package --native-image` | Not done now. tasty-query reads the JRE classes through the `jrt:/` file system and reads TASTy files at run time. A native image needs reflection and resource configuration for this, and a GraalVM build in CI. We did not test it. The gain (no JVM) does not pay for this work now. |
| Coursier `cs launch` / `cs install` | `cs install` needs an app descriptor (channel file) whose dependencies come from a Maven repository. We have none now, so it does not work with GitHub Releases only. `cs launch` also needs Maven coordinates. |
| Maven Central | A later option. It needs a Sonatype account, signed artifacts and a published POM. After that, `cs install` with a channel file works. |

## Selected option

The release artifact is the file `crap4s`. It is an assembly JAR with a launcher preamble. Run it directly or with `java -jar crap4s`.

## Build locally

```sh
scripts/package.sh
dist/crap4s --help
```

The script writes `dist/crap4s` and runs `--help` as a check.

## Make a release

1. Merge all changes to `main`.
2. Create a tag that starts with `v`:

   ```sh
   git tag v0.1.0
   git push origin v0.1.0
   ```

3. The workflow `.github/workflows/release.yml` builds the artifact. It creates the GitHub Release for the tag and attaches `crap4s`.
