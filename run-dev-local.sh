#!/usr/bin/env bash

set -euo pipefail
cd "$(dirname "$0")"

export JAVA_HOME="$PWD/.toolchain/jdk8"
export PATH="$JAVA_HOME/bin:$PATH"

if [ ! -x "$JAVA_HOME/bin/java" ]; then
    echo "No local JDK 8 found at $JAVA_HOME -- run build-dev-local.sh first." >&2
    exit 1
fi

exec bin/msoyserver "$@"
