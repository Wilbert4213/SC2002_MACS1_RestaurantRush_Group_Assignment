#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/scripts/java-env.sh"
bash "$PROJECT_ROOT/build.sh"
exec "$JAVA_BIN" -cp build/classes restaurantrush.RestaurantRush
