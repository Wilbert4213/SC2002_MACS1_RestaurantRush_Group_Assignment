#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/scripts/java-env.sh"
mkdir -p build/classes
# Remove stale compiled classes so stage changes cannot leave old features behind.
rm -rf build/classes/restaurantrush
"$JAVAC_BIN" --release 17 -Xlint:all -Werror -d build/classes src/main/java/restaurantrush/*.java
