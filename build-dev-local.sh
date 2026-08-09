#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
REPO_ROOT="$PWD"

./setup-dev-local.sh

export JAVA_HOME="$REPO_ROOT/.toolchain/jdk8"
export PATH="$JAVA_HOME/bin:$PATH"

ant -Dflexsdk.dir="$REPO_ROOT/flex3" -Dmaven.repo.remote=https://repo1.maven.org/maven2 \
    -Ddeployment=test -Ddev_deployment=true \
    -Dmsoy.user=msoy -Dburl.user=msoy -Dmsoy.group=msoy \
    distall
