#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
WILDFLY_HOME="${WILDFLY_HOME:-/Users/hipeoplea/Downloads/wildfly-39.0.1.Final}"
JAVA17_HOME="${JAVA17_HOME:-/Users/hipeoplea/Library/Java/JavaVirtualMachines/corretto-17.0.16/Contents/Home}"

export JAVA_HOME="$JAVA17_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

: "${ERPNEXT_BASE_URL:?Set ERPNEXT_BASE_URL}"
: "${ERPNEXT_API_KEY:?Set ERPNEXT_API_KEY}"
: "${ERPNEXT_API_SECRET:?Set ERPNEXT_API_SECRET}"

export ERPNEXT_RA_RAR="${ERPNEXT_RA_RAR:-$PROJECT_ROOT/erpnext-ra/build/distributions/erpnext-ra-0.0.1-SNAPSHOT.rar}"

(cd "$PROJECT_ROOT" && ./gradlew -g .gradle-local clean war)
(cd "$PROJECT_ROOT" && ./gradlew -g .gradle-local -p erpnext-ra clean rar)

"$WILDFLY_HOME/bin/jboss-cli.sh" --connect --file="$PROJECT_ROOT/docs/wildfly/install-erpnext-ra.cli"
"$WILDFLY_HOME/bin/jboss-cli.sh" --connect --commands="deploy $PROJECT_ROOT/build/libs/core-service.war --force"
