#!/usr/bin/env bash
# Runs crap4s on its own code. Exits with a code that is not 0 if a unit has CRAP 4 or more.
set -euo pipefail
cd "$(dirname "$0")/.."
root="$PWD"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
mkdir -p "$work/coverage"
opts=(-O "-coverage-out:$work/coverage" -O "-sourceroot:$root")
# Compile before the tests. A later compile with these options resets the coverage directory.
cp -r "$(scala-cli compile . "${opts[@]}" --print-class-path | cut -d: -f1)" "$work/classes"
scala-cli test . "${opts[@]}"
report="$work/report.txt"
code=0
scala-cli run . -- --tasty "$work/classes" --coverage "$work/coverage" --threshold 3.9999 --all >"$report" || code=$?
cat "$report"
# The last line is the summary: "PASS <units> max=<crap>" or "FAIL <failing>/<units> max=<crap>".
units=$(tail -n 1 "$report" | sed -nE 's/^(PASS|FAIL) ([0-9]+\/)?([0-9]+) max=.*$/\3/p')
if [ -z "$units" ] || [ "$units" -le 0 ]; then
  echo "Self-check failed: crap4s analyzed 0 units." >&2
  exit 1
fi
echo "Self-check analyzed $units units." >&2
exit "$code"
