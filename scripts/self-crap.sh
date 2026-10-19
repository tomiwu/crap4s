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
report="$work/report.csv"
code=0
scala-cli run . -- --tasty "$work/classes" --coverage "$work/coverage" --threshold 3.9999 --all >"$report" || code=$?
cat "$report"
# The first line is the CSV header. Every other line is one unit.
units=$(($(wc -l <"$report") - 1))
if [ "$units" -le 0 ]; then
  echo "Self-check failed: crap4s analyzed 0 units." >&2
  exit 1
fi
echo "Self-check analyzed $units units." >&2
exit "$code"
