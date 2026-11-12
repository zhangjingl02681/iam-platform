#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/frontend"
if [[ ! -d node_modules ]]; then
  if [[ -f package-lock.json ]]; then
    npm ci
  else
    npm install
  fi
fi
npm run build
DEST="$ROOT/target/classes/static"
rm -rf "$DEST"
mkdir -p "$DEST"
cp -R dist/. "$DEST/"
