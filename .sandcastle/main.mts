// crap4s Sandcastle loop: implement -> review -> check -> PR -> merge.
//
// For each iteration, the host does these steps:
//   1. Select the lowest-numbered open issue with the "Sandcastle" label
//      that has no open "Blocked by #N" issue.
//   2. Create a sandbox on the branch "sandcastle/issue-<N>".
//   3. Run the implementer agent, then the reviewer agent.
//   4. Run the checks in the sandbox: `scala-cli test .` and, if it exists,
//      `scripts/self-crap.sh` (every unit must have CRAP < 4).
//   5. If a check fails, run the fixer agent. Retry up to MAX_FIX_ATTEMPTS.
//   6. Push the branch, open a PR, wait for CI, squash-merge, close the issue.
// The loop stops when no issue is ready, or when an issue cannot pass.
//
// Usage: MAX_ITERATIONS=10 npx tsx .sandcastle/main.mts

import * as sandcastle from "@ai-hero/sandcastle";
import { podman } from "@ai-hero/sandcastle/sandboxes/podman";
import { execFileSync, spawnSync } from "node:child_process";

const MAX_ITERATIONS = Number(process.env.MAX_ITERATIONS ?? "10");
const MAX_FIX_ATTEMPTS = Number(process.env.MAX_FIX_ATTEMPTS ?? "3");
const MODEL = process.env.SANDCASTLE_MODEL ?? "composer-2";
const LABEL = "Sandcastle";

const hooks = {
  sandbox: {
    onSandboxReady: [
      { command: "if [ -f project.scala ]; then scala-cli compile . --test || true; fi" },
    ],
  },
};

function sh(cmd: string, args: string[]): string {
  return execFileSync(cmd, args, { encoding: "utf8", stdio: ["ignore", "pipe", "inherit"] }).trim();
}

interface Issue { number: number; title: string; body: string }

function nextIssue(): Issue | undefined {
  const open: Issue[] = JSON.parse(
    sh("gh", ["issue", "list", "--state", "open", "--label", LABEL, "--limit", "100", "--json", "number,title,body"]),
  );
  const openNumbers = new Set(open.map((i) => i.number));
  const ready = open.filter((i) => {
    const blockers = [...i.body.matchAll(/Blocked by\s+((?:#\d+[\s,and]*)+)/gi)]
      .flatMap((m) => [...m[1].matchAll(/#(\d+)/g)].map((n) => Number(n[1])));
    return blockers.every((b) => !openNumbers.has(b));
  });
  ready.sort((a, b) => a.number - b.number);
  if (open.length && !ready.length) console.log("All open issues are blocked.");
  return ready[0];
}

async function runChecks(sandbox: sandcastle.Sandbox): Promise<{ ok: boolean; log: string }> {
  const test = await sandbox.exec("scala-cli test . > /tmp/test.log 2>&1; rc=$?; tail -n 200 /tmp/test.log; exit $rc");
  const testOk = test.exitCode === 0;
  let log = `## scala-cli test .\n${test.stdout}\n`;
  let ok = testOk;
  const hasSelf = (await sandbox.exec("test -x scripts/self-crap.sh")).exitCode === 0;
  if (hasSelf) {
    const self = await sandbox.exec("scripts/self-crap.sh > /tmp/self.log 2>&1; rc=$?; tail -n 200 /tmp/self.log; exit $rc");
    log += `## scripts/self-crap.sh (CRAP < 4)\n${self.stdout}\n`;
    ok = ok && self.exitCode === 0;
  }
  return { ok, log };
}

for (let iteration = 1; iteration <= MAX_ITERATIONS; iteration++) {
  sh("git", ["checkout", "main"]);
  sh("git", ["pull", "--rebase", "origin", "main"]);
  const issue = nextIssue();
  if (!issue) { console.log("No ready issue. Stopping."); break; }
  console.log(`\n=== Iteration ${iteration}/${MAX_ITERATIONS}: issue #${issue.number} ${issue.title} ===\n`);

  const branch = `sandcastle/issue-${issue.number}`;
  const sandbox = await sandcastle.createSandbox({ branch, sandbox: podman(), hooks });
  const args = { ISSUE: String(issue.number), BRANCH: branch };
  let passed = false;
  try {
    const implement = await sandbox.run({
      name: "implementer", maxIterations: 1, agent: sandcastle.cursor(MODEL),
      promptFile: "./.sandcastle/implement-prompt.md", promptArgs: args,
    });
    if (!implement.commits.length) { console.log(`No commits for #${issue.number}. Stopping.`); break; }

    await sandbox.run({
      name: "reviewer", maxIterations: 1, agent: sandcastle.cursor(MODEL),
      promptFile: "./.sandcastle/review-prompt.md", promptArgs: args,
    });

    for (let attempt = 0; attempt <= MAX_FIX_ATTEMPTS; attempt++) {
      const result = await runChecks(sandbox);
      if (result.ok) { passed = true; break; }
      console.log(`Checks failed for #${issue.number} (attempt ${attempt}).`);
      if (attempt === MAX_FIX_ATTEMPTS) break;
      await sandbox.run({
        name: `fixer-${attempt + 1}`, maxIterations: 1, agent: sandcastle.cursor(MODEL),
        promptFile: "./.sandcastle/fix-prompt.md",
        promptArgs: { ...args, CHECK_LOG: result.log.slice(-12000) },
      });
    }
  } finally {
    await sandbox.close();
  }

  if (!passed) {
    console.log(`Issue #${issue.number} cannot pass the checks. Stopping the loop.`);
    sh("gh", ["issue", "comment", String(issue.number), "--body",
      `Sandcastle stopped. The checks failed after ${MAX_FIX_ATTEMPTS} fix attempts. Branch: \`${branch}\`.`]);
    sh("git", ["push", "-f", "origin", branch]);
    process.exitCode = 1;
    break;
  }

  sh("git", ["push", "-f", "origin", branch]);
  const url = sh("gh", ["pr", "create", "--base", "main", "--head", branch,
    "--title", issue.title,
    "--body", `This PR implements #${issue.number}.\n\nThe checks pass: \`scala-cli test .\` and the CRAP self-check (if available).\n\nCloses #${issue.number}`]);
  console.log(`PR: ${url}`);
  const probe = spawnSync("gh", ["pr", "checks", url], { encoding: "utf8" });
  const noChecks = /no checks reported/i.test(`${probe.stdout}${probe.stderr}`);
  if (!noChecks) {
    const watch = spawnSync("gh", ["pr", "checks", url, "--watch", "--fail-fast"], { stdio: "inherit", timeout: 30 * 60_000 });
    if (watch.status !== 0) {
      console.log(`CI failed for ${url}. Stopping the loop.`);
      process.exitCode = 1;
      break;
    }
  }
  sh("gh", ["pr", "merge", url, "--squash", "--delete-branch"]);
  const state = sh("gh", ["issue", "view", String(issue.number), "--json", "state", "--jq", ".state"]);
  if (state !== "CLOSED") sh("gh", ["issue", "close", String(issue.number), "--comment", `Merged in ${url}.`]);
  console.log(`Merged ${url}. Issue #${issue.number} is closed.`);
}

console.log("\nAll done.");
