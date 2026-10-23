# Task

You work on crap4s, issue #{{ISSUE}}, branch `{{BRANCH}}`. The checks failed (local checks or GitHub Actions CI). Fix the code so that all checks pass. The CI workflow is `.github/workflows/ci.yml`. Keep the job id `test`. Do not skip a CI step when a file is missing.

## Check output

```
{{CHECK_LOG}}
```

# Rules

- Find the cause of each failure. Fix the cause. Do not remove or weaken tests to make them pass.
- If a unit has CRAP 4 or more, make the method smaller or add tests to increase coverage.
- No exception may escape. Every failure is a typed `Error` case in `Either`. Do not write `throw`, an unwrapped `try`, `.get`, `.head`, `.last`, `.tail`, `.reduce`, `.toInt`, `.toDouble`, `require`, `assert` or `???`.
- Do side effects (exit, print, console, env, files, `var`, mutable collections) only in capability implementations at the edge. The allowed files are in `.scalafix.conf`. Core and pipeline are pure.
- Only `Main` has a last-resort guard (exit 2, one line, no stack trace).
- The Scalafix rules `NoThrow`, `NoUncheckedTry`, `NoUnsafeCall` and `SideEffectsAtEdge` check these rules. Run `scala-cli --power fix --check .`. Do not add a file to the allow lists to pass the check.
- Follow `docs/PRD.md`, `docs/CODING_STANDARDS.md` (when it exists) and `.sandcastle/CODING_STANDARDS.md`.
- If the format check fails, run `scala-cli fmt .`. Do not format files by hand. Then run `scala-cli fmt --check .`.
- Run `scala-cli test .` and, if it exists, `scripts/self-crap.sh`.
- Make one commit. Start the message with `RALPH: #{{ISSUE}} fix`. Write it in English, in ASD-STE100 Simplified Technical English.
- Do not push. Do not open a PR. Do not close the issue.

When you are done, output:

<promise>COMPLETE</promise>
