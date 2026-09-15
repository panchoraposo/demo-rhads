#!/usr/bin/env bash
# Install JBang + Camel CLI for Dev Spaces. Safe to re-run.
# Dev Spaces sets DISPLAY=:0; JBang then opens a Swing trust dialog, cannot
# reach X11, and reports "No alias found" / "camel@apache/camel" not found.
set -euo pipefail

CAMEL_VERSION="${CAMEL_VERSION:-4.18.3}"
export JBANG_DIR="${HOME}/.jbang"
mkdir -p "${JBANG_DIR}/bin"
export PATH="${JBANG_DIR}/bin:${PATH}"

if [[ -d /usr/lib/jvm/java-21-openjdk ]]; then
  export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
  export PATH="${JAVA_HOME}/bin:${PATH}"
elif [[ -d /usr/lib/jvm/java-17-openjdk ]]; then
  export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
  export PATH="${JAVA_HOME}/bin:${PATH}"
fi

unset DISPLAY
export JAVA_TOOL_OPTIONS="-Djava.awt.headless=true${JAVA_TOOL_OPTIONS:+ ${JAVA_TOOL_OPTIONS}}"

if ! command -v jbang >/dev/null 2>&1; then
  echo "Installing JBang..."
  curl -Ls https://sh.jbang.dev | env -u DISPLAY bash -s - app setup
  export PATH="${JBANG_DIR}/bin:${PATH}"
fi

if ! grep -q '.jbang/bin' "${HOME}/.bashrc" 2>/dev/null; then
  echo 'export PATH="$HOME/.jbang/bin:$PATH"' >> "${HOME}/.bashrc"
fi

if ! command -v camel >/dev/null 2>&1; then
  echo "Installing Camel CLI via JBang (trust GitHub, no GUI)..."
  jbang trust add https://github.com/apache/camel
  jbang app install --force --name camel camel@apache/camel
  hash -r || true
fi

if ! command -v camel >/dev/null 2>&1; then
  echo "Camel CLI not found on PATH after install (${JBANG_DIR}/bin)." >&2
  ls -la "${JBANG_DIR}/bin" >&2 || true
  exit 1
fi

camel version set "${CAMEL_VERSION}" || true
echo "jbang=$(command -v jbang)"
echo "camel=$(command -v camel)"
camel version 2>/dev/null | head -n 3 || true
