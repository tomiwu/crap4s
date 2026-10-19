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
// Usage:
//   MAX_ITERATIONS=10 npx tsx .sandcastle/main.mts                (default: Claude Code, Sonnet 5.5)
//   AGENT=cursor MAX_ITERATIONS=10 npx tsx .sandcastle/main.mts   (option: Cursor, Composer 2.5 Fast)
// If the Cursor agent fails because of credits or limits, the loop changes to Claude.

import * as sandcastle from "@ai-hero/sandcastle";
import { podman } from "@ai-hero/sandcastle/sandboxes/podman";
import { execFileSync, spawnSync } from "node:child_process";
import { readdirSync, readFileSync, statSync } from "node:fs";
import { join } from "node:path";

const MAX_ITERATIONS = Number(process.env.MAX_ITERATIONS ?? "10");
const MAX_FIX_ATTEMPTS = Number(process.env.MAX_FIX_ATTEMPTS ?? "3");
let agentName = (process.env.AGENT ?? "claude") === "cursor" ? "cursor" : "claude";
const CURSOR_MODEL = process.env.CURSOR_MODEL ?? "composer-2.5-fast";
const CLAUDE_MODEL = process.env.CLAUDE_MODEL ?? "claude-sonnet-5-5";
const LIMIT_ERROR = /credit|quota|usage limit|rate limit|payment|out of (fast )?requests|insufficient/i;

function agent() {
  return agentName === "claude" ? sandcastle.claudeCode(CLAUDE_MODEL) : sandcastle.cursor(CURSOR_MODEL);
}

type RunOptions = Omit<Parameters<sandcastle.Sandbox["run"]>[0], "agent">;

// Run one agent phase. Change from Cursor to Claude if Cursor has no credits.
async function runPhase(sandbox: sandcastle.Sandbox, options: RunOptions) {
  try {
    const result = await sandbox.run({ ...options, agent: agent() });
    if (agentName === "cursor" && !result.commits.length && logHasLimitError(options.name)) throw new Error("cursor limit");
    return result;
  } catch (error) {
    const text = `${error}`;
    if (agentName === "cursor" && (LIMIT_ERROR.test(text) || logHasLimitError(options.name))) {
      console.log(`Cursor failed (${text.slice(0, 200)}). Change to Claude Code and run again.`);
      agentName = "claude";
      return sandbox.run({ ...options, agent: agent() });
    }
    throw error;
  }
}

function logHasLimitError(name?: string): boolean {
  try {
    const dir = ".sandcastle/logs";
    const files = readdirSync(dir).filter((f) => name && f.endsWith(`-${name}.log`))
      .map((f) => join(dir, f)).sort((a, b) => statSync(b).mtimeMs - statSync(a).mtimeMs);
    return files.length > 0 && LIMIT_ERROR.test(readFileSync(files[0], "utf8").slice(-4000));
  } catch {
    return false;
  }
}
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

function nextIssue(merged: Set<number>): Issue | undefined {
  const open: Issue[] = JSON.parse(
    sh("gh", ["issue", "list", "--state", "open", "--label", LABEL, "--limit", "100", "--json", "number,title,body"]),
  );
  const openNumbers = new Set(open.map((i) => i.number).filter((n) => !merged.has(n)));
  open.splice(0, open.length, ...open.filter((i) => !merged.has(i.number)));
  const ready = open.filter((i) => {
    const blockers = [...i.body.matchAll(/Blocked by\s+((?:#\d+[\s,and]*)+)/gi)]
      .flatMap((m) => [...m[1].matchAll(/#(\d+)/g)].map((n) => Number(n[1])));
    return blockers.every((b) => !openNumbers.has(b));
  });
  ready.sort((a, b) => a.number - b.number);
  if (open.length && !ready.length) console.log("All open issues are blocked.");
  // The issue list can be stale just after a merge. Check the state of the issue again.
  return ready.find((i) => sh("gh", ["issue", "view", String(i.number), "--json", "state", "--jq", ".state"]) === "OPEN");
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

const merged = new Set<number>();
console.log(`Agent: ${agentName}`);

for (let iteration = 1; iteration <= MAX_ITERATIONS; iteration++) {
  sh("git", ["checkout", "main"]);
  sh("git", ["pull", "--rebase", "origin", "main"]);
  let issue = nextIssue(merged);
  if (!issue) {
    // The issue list can be stale just after a merge. Wait and try again one time.
    await new Promise((r) => setTimeout(r, 30_000));
    issue = nextIssue(merged);
  }
  if (issue && merged.has(issue.number)) { console.log(`Issue #${issue.number} is already merged. Stopping.`); break; }
  if (!issue) { console.log("No ready issue. Stopping."); break; }
  console.log(`\n=== Iteration ${iteration}/${MAX_ITERATIONS}: issue #${issue.number} ${issue.title} ===\n`);

  const branch = `sandcastle/issue-${issue.number}-${Date.now()}`;
  const sandbox = await sandcastle.createSandbox({ branch, sandbox: podman(), hooks });
  const args = { ISSUE: String(issue.number), BRANCH: branch };
  let passed = false;
  try {
    const implement = await runPhase(sandbox, {
      name: "implementer", maxIterations: 1,
      promptFile: "./.sandcastle/implement-prompt.md", promptArgs: args,
    });
    if (!implement.commits.length) { console.log(`No commits for #${issue.number}. Stopping.`); break; }

    await runPhase(sandbox, {
      name: "reviewer", maxIterations: 1,
      promptFile: "./.sandcastle/review-prompt.md", promptArgs: args,
    });

    for (let attempt = 0; attempt <= MAX_FIX_ATTEMPTS; attempt++) {
      const result = await runChecks(sandbox);
      if (result.ok) { passed = true; break; }
      console.log(`Checks failed for #${issue.number} (attempt ${attempt}).`);
      if (attempt === MAX_FIX_ATTEMPTS) break;
      await runPhase(sandbox, {
        name: `fixer-${attempt + 1}`, maxIterations: 1,
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
  // Merge only when all GitHub Actions checks on the PR pass.
  let hasChecks = false;
  for (let wait = 0; wait < 12 && !hasChecks; wait++) {
    await new Promise((r) => setTimeout(r, 15_000));
    const probe = spawnSync("gh", ["pr", "checks", url], { encoding: "utf8" });
    hasChecks = !/no checks reported/i.test(`${probe.stdout}${probe.stderr}`);
  }
  if (!hasChecks) {
    console.log(`No CI checks started for ${url}. Do not merge. Stopping the loop.`);
    process.exitCode = 1;
    break;
  }
  const watch = spawnSync("gh", ["pr", "checks", url, "--watch", "--fail-fast"], { stdio: "inherit", timeout: 30 * 60_000 });
  if (watch.status !== 0) {
    console.log(`CI failed for ${url}. Do not merge. Stopping the loop.`);
    process.exitCode = 1;
    break;
  }
  sh("gh", ["pr", "merge", url, "--squash", "--delete-branch"]);
  const state = sh("gh", ["issue", "view", String(issue.number), "--json", "state", "--jq", ".state"]);
  if (state !== "CLOSED") sh("gh", ["issue", "close", String(issue.number), "--comment", `Merged in ${url}.`]);
  console.log(`Merged ${url}. Issue #${issue.number} is closed.`);
  spawnSync("git", ["branch", "-D", branch]);
  merged.add(issue.number);
}

console.log("\nAll done.");
