# Context

You work on crap4s. crap4s is a Scala CLI tool. It calculates the CRAP score (Uncle Bob) for Scala 3 code. It reads TASTy files with tasty-query and coverage from the Scala 3 compiler option `-coverage-out`.

## Your issue: #{{ISSUE}}

!`gh issue view {{ISSUE}} --json number,title,body,comments --jq '{number, title, body, comments: [.comments[].body]}'`

## Recent commits

!`git log --oneline -10`

# Task

Implement issue #{{ISSUE}} on branch `{{BRANCH}}`. Do only this issue.

1. Read the issue. Read `docs/PRD.md`. Follow the PRD. Follow `docs/CODING_STANDARDS.md` when it exists.
2. Read the source files and tests that exist.
3. Use Red-Green-Refactor. Write a failing test first. Then write the code that makes the test pass.
4. Run `scala-cli compile .` and `scala-cli test .`. Fix all failures.
5. Make one git commit. Start the message with `RALPH: #{{ISSUE}}`. Write the task, the key decisions and the files changed.

# Rules

- Write all code, comments and commit messages in English. Use ASD-STE100 Simplified Technical English: short sentences, active voice, one instruction in each sentence.
- Keep all versions in `project.scala` as `//> using` directives. Use these versions: Scala `3.10.0`, `ch.epfl.scala::tasty-query:1.10.0`, `org.scalameta::munit:1.3.6`, JVM `25`. If you add a library, use its latest stable version.
- Do not push. Do not open a PR. Do not close the issue. The host does these steps after the checks pass.
- Do not edit `docs/PRD.md`, `.sandcastle/` or `.github/`.
- Do not leave commented-out code or TODO comments.
- crap4s checks its own code. Every unit (method or top-level lambda) must have a CRAP score less than 4. Keep methods small and tested.
- When `crap4s` can run end to end (issue #9 or later), make sure `scripts/self-crap.sh` exists and is executable. The script must: compile and test crap4s with `-coverage-out`, run crap4s on its own TASTy and coverage output, and exit with a code that is not 0 if a unit has CRAP 4 or more. CI (`.github/workflows/ci.yml`) runs this script when it exists.
- If you cannot continue, write a comment on the issue with `gh issue comment {{ISSUE}}`. Explain the problem.

When you are done, output:

<promise>COMPLETE</promise>
