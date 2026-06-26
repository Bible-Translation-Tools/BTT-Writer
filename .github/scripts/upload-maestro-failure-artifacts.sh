#!/usr/bin/env bash
set -euo pipefail

OUTPUT_DIR="${1:-$HOME/.maestro/tests}"
API_URL="https://tmpfiles.org/api/v1/upload"
EXPIRE_SECONDS="${TMPFILES_EXPIRE_SECONDS:-86400}"

upload_file() {
  local file="$1"
  local label="$2"

  if [ ! -f "$file" ]; then
    echo "Missing file for upload ($label): $file"
    return 1
  fi

  echo "::group::Upload Maestro $label to tmpfiles.org"
  echo "Local file: $file ($(du -h "$file" | cut -f1))"

  local response
  if ! response="$(curl -fsS -F "file=@${file}" -F "expire=${EXPIRE_SECONDS}" "$API_URL")"; then
    echo "tmpfiles.org upload failed for $file"
    echo "::endgroup::"
    return 1
  fi

  echo "tmpfiles.org response: $response"

  local page_url direct_url
  page_url="$(printf '%s' "$response" | python3 -c "import json,sys; data=json.load(sys.stdin); print(data.get('data',{}).get('url',''))" 2>/dev/null || true)"
  if [ -z "$page_url" ]; then
    page_url="$(printf '%s' "$response" | sed -n 's/.*"url"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)"
  fi

  if [ -n "$page_url" ]; then
    direct_url="${page_url/tmpfiles.org\//tmpfiles.org/dl/}"
    echo "Maestro failure $label (page): $page_url"
    echo "Maestro failure $label (direct image): $direct_url"
  else
    echo "Could not parse tmpfiles.org URL from response"
  fi

  echo "::endgroup::"
}

find_latest_run_dir() {
  local base_dir="$1"
  if [ ! -d "$base_dir" ]; then
    return 1
  fi

  find "$base_dir" -mindepth 1 -maxdepth 3 -type d -printf '%T@ %p\n' 2>/dev/null \
    | sort -rn \
    | head -1 \
    | cut -d' ' -f2-
}

find_failure_screenshot() {
  local search_dir="$1"
  if [ ! -d "$search_dir" ]; then
    return 1
  fi

  find "$search_dir" -type f \( -name '*.png' -o -name '*.jpg' -o -name '*.jpeg' \) -printf '%T@ %p\n' 2>/dev/null \
    | sort -rn \
    | head -1 \
    | cut -d' ' -f2-
}

RUN_DIR="$(find_latest_run_dir "$OUTPUT_DIR" || true)"
if [ -z "${RUN_DIR:-}" ] && [ -d "$HOME/.maestro/tests" ]; then
  RUN_DIR="$(find_latest_run_dir "$HOME/.maestro/tests" || true)"
fi

SCREENSHOT=""
if [ -n "${RUN_DIR:-}" ]; then
  echo "Maestro artifact directory: $RUN_DIR"
  find "$RUN_DIR" -maxdepth 3 -type f \( -name '*.png' -o -name '*.jpg' -o -name '*.json' -o -name '*.log' \) -print || true
  SCREENSHOT="$(find_failure_screenshot "$RUN_DIR" || true)"
fi

if [ -z "${SCREENSHOT:-}" ] && command -v adb >/dev/null 2>&1; then
  FALLBACK="/tmp/maestro-failure-screenshot.png"
  echo "No Maestro screenshot found; capturing adb screencap fallback"
  if adb exec-out screencap -p > "$FALLBACK" 2>/dev/null && [ -s "$FALLBACK" ]; then
    SCREENSHOT="$FALLBACK"
  fi
fi

if [ -z "${SCREENSHOT:-}" ]; then
  echo "No failure screenshot available to upload"
  exit 0
fi

upload_file "$SCREENSHOT" "screenshot"
