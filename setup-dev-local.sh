#!/usr/bin/env bash

set -euo pipefail
cd "$(dirname "$0")"
REPO_ROOT="$PWD"

JDK_DIR="$REPO_ROOT/.toolchain/jdk8"
JDK_URL="https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u342-b07/OpenJDK8U-jdk_x64_linux_hotspot_8u342b07.tar.gz"

# Sandbox JDK 8 in a project-local directory -- the system's default `java`
# (and JAVA_HOME outside this script) is never touched.
if [ ! -x "$JDK_DIR/bin/java" ]; then
    echo "Fetching local JDK 8 (Temurin 8u342-b07)..."
    mkdir -p "$JDK_DIR"
    curl -fL "$JDK_URL" | tar xz -C "$JDK_DIR" --strip-components=1
fi

export JAVA_HOME="$JDK_DIR"
export PATH="$JAVA_HOME/bin:$PATH"

# Jetty's parent pom isn't on Maven Central; install it into ~/.m2 once.
if [ ! -f "$HOME/.m2/repository/org/eclipse/jetty/jetty-parent/17/jetty-parent-17.pom" ]; then
    echo "Installing jetty-parent pom into ~/.m2..."
    curl -fL -o /tmp/jetty-parent-17.pom https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-parent/17/jetty-parent-17.pom
    mvn install:install-file \
        -Dfile=/tmp/jetty-parent-17.pom \
        -DgroupId=org.eclipse.jetty \
        -DartifactId=jetty-parent \
        -Dversion=17 \
        -Dpackaging=pom
    rm /tmp/jetty-parent-17.pom
fi

# Pre-bundled dependencies with custom groupIds (not on Central) -- cheap to
# re-extract into ~/.m2 every run, keeps it in sync with artifacts.jar.
mkdir -p "$HOME/.m2"
(cd "$HOME" && jar xf "$REPO_ROOT/artifacts.jar")

# Flex SDK, kept at the repo-root path build.properties already expects.
if [ ! -d "$REPO_ROOT/flex3/frameworks" ]; then
    echo "Fetching Flex SDK..."
    curl -fL -o /tmp/flex_sdk.zip http://fpdownload.adobe.com/pub/flex/sdk/builds/flex3/flex_sdk_3.3.0.4852.zip
    mkdir -p "$REPO_ROOT/flex3"
    unzip -q /tmp/flex_sdk.zip -d "$REPO_ROOT/flex3"
    rm /tmp/flex_sdk.zip
fi

if [ ! -f "$REPO_ROOT/lib/gwt-asyncgen.jar" ]; then
    echo "Fetching gwt-asyncgen.jar..."
    curl -fL -o "$REPO_ROOT/lib/gwt-asyncgen.jar" https://repo1.maven.org/maven2/com/samskivert/gwt-asyncgen/1.0/gwt-asyncgen-1.0.jar
fi

echo "Toolchain ready: JAVA_HOME=$JDK_DIR"
