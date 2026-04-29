#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
WILDFLY_HOME="${WILDFLY_HOME:-/Users/hipeoplea/Downloads/wildfly-39.0.1.Final}"
JAVA17_HOME="${JAVA17_HOME:-/Users/hipeoplea/Library/Java/JavaVirtualMachines/corretto-17.0.16/Contents/Home}"
BLPS_ACCOUNTS_XML="${BLPS_ACCOUNTS_XML:-$PROJECT_ROOT/data/security/accounts.xml}"

: "${ERPNEXT_BASE_URL:?Set ERPNEXT_BASE_URL}"
: "${ERPNEXT_API_KEY:?Set ERPNEXT_API_KEY}"
: "${ERPNEXT_API_SECRET:?Set ERPNEXT_API_SECRET}"

export JAVA_HOME="$JAVA17_HOME"
export PATH="$JAVA_HOME/bin:$PATH"
export ERPNEXT_BASE_URL
export ERPNEXT_API_KEY
export ERPNEXT_API_SECRET
export JAVA_OPTS="${JAVA_OPTS:-} -Dblps.security.accounts-location=$BLPS_ACCOUNTS_XML"

exec "$WILDFLY_HOME/bin/standalone.sh"
