#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/scripts/java-env.sh"
bash "$PROJECT_ROOT/build.sh"
mkdir -p build/test-classes
rm -rf build/test-classes/restaurantrush
"$JAVAC_BIN" --release 17 -Xlint:all -Werror -cp build/classes -d build/test-classes src/test/java/restaurantrush/*.java
"$JAVA_BIN" -cp build/classes:build/test-classes restaurantrush.Stage1Tests
