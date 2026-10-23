#!/usr/bin/env bash
# Runs the artifact with bad inputs. Each run must exit with 2 and print one clean line on stderr.
# Usage: scripts/bad-inputs.sh <artifact> <tasty-dir> <coverage-dir>
set -uo pipefail
artifact="$1"
tasty="$2"
coverage="$3"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

mkdir "$work/bad-coverage" "$work/bad-measurements"
cp "$coverage"/* "$work/bad-coverage/"
cp "$coverage"/* "$work/bad-measurements/"
echo garbage >"$work/bad-coverage/scoverage.coverage"
echo not-a-number >"$work/bad-measurements/scoverage.measurements.1"
echo "this is not a jar" >"$work/corrupt.jar"

fail=0
check() {
  local name="$1"
  shift
  local err code
  err="$("$artifact" "$@" 2>&1 >"$work/out")"
  code=$?
  if [ "$code" -ne 2 ] || [ -s "$work/out" ] \
    || [ "$(printf '%s\n' "$err" | wc -l)" -ne 1 ] \
    || [[ "$err" != crap4s:* ]] \
    || [[ "$err" == *Exception* ]] || [[ "$err" == *$'\tat '* ]] || [[ "$err" == *"at crap4s."* ]]; then
    echo "Bad inputs failed: $name (exit $code): $err" >&2
    fail=1
  fi
}

check "missing tasty directory" --tasty "$work/none" --coverage "$coverage"
check "missing coverage directory" --tasty "$tasty" --coverage "$work/none"
check "broken scoverage.coverage" --tasty "$tasty" --coverage "$work/bad-coverage"
check "broken measurements file" --tasty "$tasty" --coverage "$work/bad-measurements"
check "corrupt jar" --tasty "$tasty" --coverage "$coverage" --classpath "$work/corrupt.jar"
check "missing classpath file" --tasty "$tasty" --coverage "$coverage" --classpath-file "$work/none.txt"
check "invalid path" --tasty "$tasty" --coverage "$coverage" --classpath-file $'bad\0path'
[ "$fail" -eq 0 ] && echo "All bad inputs gave exit 2 and one clean line."
exit "$fail"
