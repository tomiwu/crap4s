#!/usr/bin/env bash
# End-to-end test of the release artifact on a sample project with an external dependency.
# Usage: scripts/e2e-release.sh [artifact]
# Without an argument, the script builds the artifact with scripts/package.sh.
set -euo pipefail
cd "$(dirname "$0")/.."
root="$PWD"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

artifact="${1:-}"
if [ -z "$artifact" ]; then
  artifact="$work/crap4s"
  "$root/scripts/package.sh" "$artifact"
fi

sample="$work/sample"
cp -r "$root/samples/with-dependency" "$sample"
cd "$sample"
opts=(-O "-coverage-out:$work/coverage" -O "-sourceroot:$sample")
# Compile before the tests. A later compile with these options resets the coverage directory.
scala-cli compile . "${opts[@]}" -d "$work/classes"
scala-cli compile . "${opts[@]}" --print-class-path >"$work/classpath.txt"
scala-cli test . "${opts[@]}"

report="$work/report.txt"
"$artifact" --tasty "$work/classes" --coverage "$work/coverage" \
  --classpath-file "$work/classpath.txt" --threshold 6 --all >"$report"
cat "$report"

fail=0
if ! tail -n 1 "$report" | grep -qE '^(PASS|FAIL) '; then
  echo "E2E failed: the summary line is wrong." >&2
  fail=1
fi
for unit in extension countLines; do
  if ! grep -q "$unit" "$report"; then
    echo "E2E failed: the unit $unit is missing in the report." >&2
    fail=1
  fi
done
exit "$fail"
