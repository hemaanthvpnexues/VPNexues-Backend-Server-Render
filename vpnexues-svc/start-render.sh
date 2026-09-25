#!/bin/sh
# Portable start for Render/Docker when you prefer a script over an inline startCommand.
# Prefer the plain startCommand in render.yaml unless you need this.
set -e
JAR=$(ls build/libs/vpnexues-svc-*.jar 2>/dev/null | head -n1)
if [ -z "$JAR" ]; then
  echo "No jar in build/libs — run ./gradlew bootJar first" >&2
  exit 1
fi
# Render sets PORT (often 10000). Fallback keeps local behavior.
PORT="${PORT:-${SERVER_PORT:-8080}}"
exec java -Dserver.port="$PORT" $JAVA_OPTS -jar "$JAR"
