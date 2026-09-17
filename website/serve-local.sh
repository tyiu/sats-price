#!/usr/bin/env bash
# Builds the web app, drops it into website/app/, and serves the whole
# website locally so the /app/ links behave like they do in production.
set -euo pipefail

PORT="${1:-8000}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(dirname "$SCRIPT_DIR")"

echo "Building web app (webHtmlApp)..."
"$REPO_ROOT/gradlew" -p "$REPO_ROOT" :webHtmlApp:jsBrowserDistribution

echo "Copying web app into website/app/..."
mkdir -p "$SCRIPT_DIR/app"
cp -r "$REPO_ROOT/webHtmlApp/build/dist/js/productionExecutable/." "$SCRIPT_DIR/app/"

echo "Serving website at http://localhost:$PORT/ (web app at http://localhost:$PORT/app/)"
cd "$SCRIPT_DIR" && python3 -m http.server "$PORT"
