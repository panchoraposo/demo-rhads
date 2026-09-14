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

: "${MAAS_BASE_URL:?Set MAAS_BASE_URL to your OpenAI-compatible MaaS endpoint (…/v1)}"
: "${MAAS_API_KEY:?Set MAAS_API_KEY (command palette → Write .env for Red Hat MaaS)}"
: "${MAAS_MODEL:?Set MAAS_MODEL}"

export AI_BASE_URL="${MAAS_BASE_URL}"
export AI_API_KEY="${MAAS_API_KEY}"
export AI_MODEL="${MAAS_MODEL}"
PROPS_FILE="application-maas.properties"

echo "MaaS model: ${AI_MODEL}"
echo "MaaS URL:   ${AI_BASE_URL}"
echo "UI:         fleet-ui on port 8080 (PORTS → fleet-ui)"
echo "Kaoto:      open integrations/03-workflow.camel.yaml"
echo

camel_jbang_run "${ROOT}"
