#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck disable=SC1091
source "${ROOT}/scripts/lib-run.sh"

ensure_camel_cli
CAMEL_VERSION="${CAMEL_VERSION:-4.18.3}"

if [[ -f "${ROOT}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT}/.env"
  set +a
fi

export AI_BASE_URL="${AI_BASE_URL:-http://localhost:11434/v1}"
export AI_API_KEY="${AI_API_KEY:-ollama}"
export AI_MODEL="${AI_MODEL:-llama3.2:3b}"
PROPS_FILE="application-local.properties"

echo "Ollama model: ${AI_MODEL}"
echo "UI:           fleet-ui on port 8080"
echo

camel_jbang_run "${ROOT}"
