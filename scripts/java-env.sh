#!/usr/bin/env bash
# Sourced by the project scripts. JDK is the only external dependency.
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -n "${JAVA_HOME:-}" ]]; then
    PROJECT_JDK="$JAVA_HOME"
elif [[ -x /opt/homebrew/opt/openjdk@17/bin/javac ]]; then
    PROJECT_JDK=/opt/homebrew/opt/openjdk@17
elif [[ -x /usr/local/opt/openjdk@17/bin/javac ]]; then
    PROJECT_JDK=/usr/local/opt/openjdk@17
else
    PROJECT_JDK=""
fi
if [[ -n "$PROJECT_JDK" ]]; then
    JAVA_BIN="$PROJECT_JDK/bin/java"
    JAVAC_BIN="$PROJECT_JDK/bin/javac"
else
    JAVA_BIN=java
    JAVAC_BIN=javac
fi
if ! "$JAVAC_BIN" -version >/dev/null 2>&1; then
    echo "JDK 17 or newer is required. Install a JDK and set JAVA_HOME to its directory." >&2
    exit 1
fi
cd "$PROJECT_ROOT"
