#!/usr/bin/env bash
# Builds the release artifact: an executable assembly JAR. Usage: scripts/package.sh [output-file]
set -euo pipefail
cd "$(dirname "$0")/.."
out="${1:-dist/crap4s}"
mkdir -p "$(dirname "$out")"
scala-cli --power package --assembly . -o "$out" -f
chmod +x "$out"
"$out" --help >/dev/null
echo "Built $out"
