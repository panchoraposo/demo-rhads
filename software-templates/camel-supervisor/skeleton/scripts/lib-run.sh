#!/usr/bin/env bash
# Shared Camel JBang launcher. Sourced by run-local.sh and run-maas.sh.
# Expects: ROOT, CAMEL_VERSION, AI_BASE_URL, AI_API_KEY, AI_MODEL, PROPS_FILE

camel_jbang_run() {
  local dir="$1"
  shift
  cd "$dir"
  local files=(application.properties)
  local f
  while IFS= read -r f; do
    [[ -n "${f}" ]] && files+=("${f}")
  done < <(find src -name '*.java' ! -path '*/quarkus/*' 2>/dev/null | sort)
  while IFS= read -r f; do
    [[ -n "${f}" ]] && files+=("${f}")
  done < <(find integrations \( -name '*.yaml' -o -name '*.yml' \) 2>/dev/null | sort)
  local flags=(--camel-version="${CAMEL_VERSION}" --dev)
  if [[ "${SKIP_CONSOLE:-0}" != "1" ]]; then
    flags+=(--console --health)
  fi
  if [[ -n "${CAMEL_HTTP_PORT:-}" ]]; then
    flags+=(--port="${CAMEL_HTTP_PORT}")
  fi
  exec camel run \
    "${flags[@]}" \
    --properties="${PROPS_FILE}" \
    --property="ai.model=${AI_MODEL}" \
    --property="ai.base-url=${AI_BASE_URL}" \
    --property="ai.api-key=${AI_API_KEY}" \
    "$@" \
    "${files[@]}"
}

wait_listen() {
  local port="$1"
  local tries="${2:-60}"
  local i
  for i in $(seq 1 "${tries}"); do
    if lsof -nP -iTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  return 1
}

ensure_camel_cli() {
  local here
  here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  # shellcheck disable=SC1091
  source "${here}/install-camel-cli.sh"
}

