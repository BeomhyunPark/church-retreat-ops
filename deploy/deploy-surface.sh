#!/usr/bin/env bash
set -Eeuo pipefail

TAG="${1:-}"
SERVER_DIR="${SERVER_DIR:-$HOME/server}"
ENV_FILE="$SERVER_DIR/.env"

if [[ ! "$TAG" =~ ^[0-9a-f]{40}$ ]]; then
    echo "Usage: $0 <40-char-git-sha>" >&2
    exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
    echo "ERROR: $ENV_FILE not found" >&2
    exit 1
fi

CURRENT_TAG="$(grep '^APP_IMAGE_TAG=' "$ENV_FILE" | tail -1 | cut -d= -f2-)"

if [[ "$CURRENT_TAG" == "$TAG" ]]; then
    echo "Already deployed: $TAG"
    exit 0
fi

echo "Current: ${CURRENT_TAG:-none}"
echo "Target : $TAG"

# 새 이미지가 실제 존재하는지 먼저 검증
docker pull "ghcr.io/beomhyunpark/church-retreat-ops-backend:$TAG"
docker pull "ghcr.io/beomhyunpark/church-retreat-ops-web:$TAG"

# 한 단계 롤백용 이전 SHA 저장
if [[ -n "$CURRENT_TAG" ]]; then
    printf '%s\n' "$CURRENT_TAG" > "$SERVER_DIR/.previous-image-tag"
    chmod 600 "$SERVER_DIR/.previous-image-tag"
fi

TMP_FILE="$(mktemp)"

awk -v tag="$TAG" '
BEGIN { replaced = 0 }
/^APP_IMAGE_TAG=/ {
    if (!replaced) {
        print "APP_IMAGE_TAG=" tag
        replaced = 1
    }
    next
}
{ print }
END {
    if (!replaced)
        print "APP_IMAGE_TAG=" tag
}
' "$ENV_FILE" > "$TMP_FILE"

chmod --reference="$ENV_FILE" "$TMP_FILE"
mv "$TMP_FILE" "$ENV_FILE"

cd "$SERVER_DIR"

docker compose up -d backend caddy

echo
docker compose ps

echo
echo "Deployed:"
echo "  $TAG"
