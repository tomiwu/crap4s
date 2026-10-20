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
