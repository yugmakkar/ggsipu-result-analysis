#!/usr/bin/env bash
# Batch-import every PDF in a folder into the database.
# Usage: scripts/import_all.sh <folder-with-pdfs>
set -euo pipefail

DIR="${1:-}"
if [[ -z "$DIR" || ! -d "$DIR" ]]; then
  echo "usage: scripts/import_all.sh <folder-with-pdfs>" >&2
  exit 1
fi

JAR="target/result-analysis.jar"
if [[ ! -f "$JAR" ]]; then
  echo "Build first:  mvn -q -DskipTests package" >&2
  exit 1
fi

shopt -s nullglob
count=0
for pdf in "$DIR"/*.pdf "$DIR"/*.PDF; do
  echo ">> importing $(basename "$pdf")"
  java -jar "$JAR" import "$pdf"
  count=$((count + 1))
done

echo "Done. Imported $count file(s)."
