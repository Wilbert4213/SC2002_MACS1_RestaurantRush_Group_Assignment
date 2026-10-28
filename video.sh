#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/scripts/java-env.sh"
if [[ $# -ne 1 || ( "$1" != victory && "$1" != defeat ) ]]; then
    echo "Usage: bash video.sh victory|defeat" >&2
    exit 1
fi
bash "$PROJECT_ROOT/build.sh"
exec "$JAVA_BIN" -cp build/classes restaurantrush.VideoReplay "$1"
