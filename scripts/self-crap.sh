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
scala-cli run . -- --tasty "$work/classes" --coverage "$work/coverage" --threshold 3.9999
