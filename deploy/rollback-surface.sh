#!/usr/bin/env bash
set -Eeuo pipefail

SERVER_DIR="${SERVER_DIR:-$HOME/server}"
PREVIOUS_FILE="$SERVER_DIR/.previous-image-tag"

if [[ ! -f "$PREVIOUS_FILE" ]]; then
    echo "ERROR: no previous image tag recorded" >&2
    exit 1
fi

PREVIOUS_TAG="$(tr -d '[:space:]' < "$PREVIOUS_FILE")"

if [[ ! "$PREVIOUS_TAG" =~ ^[0-9a-f]{40}$ ]]; then
    echo "ERROR: invalid previous image tag" >&2
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Rolling back to:"
echo "  $PREVIOUS_TAG"

"$SCRIPT_DIR/deploy-surface.sh" "$PREVIOUS_TAG"
