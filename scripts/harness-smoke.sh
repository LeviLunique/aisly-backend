#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"

curl --fail --silent "$BASE_URL/healthcheck" | grep -q "OK"

if [[ -n "${AISLY_TOKEN:-}" ]]; then
  curl --fail --silent \
    -H "Authorization: Bearer $AISLY_TOKEN" \
    "$BASE_URL/api/v1/me" >/dev/null
fi

echo "harness smoke checks passed"

