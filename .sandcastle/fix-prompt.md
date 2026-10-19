# Task

You work on crap4s, issue #{{ISSUE}}, branch `{{BRANCH}}`. The checks failed. Fix the code so that all checks pass.

## Check output

```
{{CHECK_LOG}}
```

# Rules

- Find the cause of each failure. Fix the cause. Do not remove or weaken tests to make them pass.
- If a unit has CRAP 4 or more, make the method smaller or add tests to increase coverage.
- Follow `docs/PRD.md`, `docs/CODING_STANDARDS.md` (when it exists) and `.sandcastle/CODING_STANDARDS.md`.
- Run `scala-cli test .` and, if it exists, `scripts/self-crap.sh`.
- Make one commit. Start the message with `RALPH: #{{ISSUE}} fix`. Write it in English, in ASD-STE100 Simplified Technical English.
- Do not push. Do not open a PR. Do not close the issue.

When you are done, output:

<promise>COMPLETE</promise>
