#!/usr/bin/env bash
# Resolve JDK 21 in the Dev Spaces UDI (paths vary by image tag).
# Safe to `source` from the Quarkus dev command.
if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
  export PATH="${JAVA_HOME}/bin:${PATH}"
  echo "JAVA_HOME=${JAVA_HOME}"
  return 0 2>/dev/null || exit 0
fi
JAVA_HOME=""
for d in /usr/lib/jvm/java-21-openjdk /usr/lib/jvm/jre-21-openjdk /usr/lib/jvm/java-21; do
  if [ -x "${d}/bin/java" ]; then
    JAVA_HOME="${d}"
    break
  fi
done
if [ -z "${JAVA_HOME}" ]; then
  found="$(find /usr/lib/jvm -maxdepth 3 -type f -path '*21*/bin/java' 2>/dev/null | head -1 || true)"
  if [ -n "${found}" ]; then
    JAVA_HOME="$(dirname "$(dirname "${found}")")"
  fi
fi
if [ -n "${JAVA_HOME}" ]; then
  export JAVA_HOME
  export PATH="${JAVA_HOME}/bin:${PATH}"
  echo "JAVA_HOME=${JAVA_HOME} ($("${JAVA_HOME}/bin/java" -version 2>&1 | head -1))"
else
  echo "WARN: JDK 21 not found; using default $(java -version 2>&1 | head -1)"
fi
