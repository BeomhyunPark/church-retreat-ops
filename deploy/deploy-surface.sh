#!/usr/bin/env bash
set -Eeuo pipefail

TAG="${1:-}"
SERVER_DIR="${SERVER_DIR:-$HOME/server}"
ENV_FILE="$SERVER_DIR/.env"

SITE_HOST="${SITE_HOST:-retreat.greengroove.app}"
HEALTH_RETRIES="${HEALTH_RETRIES:-30}"
HEALTH_INTERVAL="${HEALTH_INTERVAL:-2}"
ROLLBACK_HEALTH_RETRIES="${ROLLBACK_HEALTH_RETRIES:-30}"
ROLLBACK_HEALTH_INTERVAL="${ROLLBACK_HEALTH_INTERVAL:-2}"

BACKEND_IMAGE="ghcr.io/beomhyunpark/church-retreat-ops-backend"
WEB_IMAGE="ghcr.io/beomhyunpark/church-retreat-ops-web"

if [[ ! "$TAG" =~ ^[0-9a-f]{40}$ ]]; then
    echo "Usage: $0 <40-char-git-sha>" >&2
    exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
    echo "ERROR: $ENV_FILE not found" >&2
    exit 1
fi

CURRENT_TAG="$(
    grep '^APP_IMAGE_TAG=' "$ENV_FILE" 2>/dev/null \
        | tail -1 \
        | cut -d= -f2- || true
)"

if [[ "$CURRENT_TAG" == "$TAG" ]]; then
    echo "Already deployed: $TAG"
    exit 0
fi

set_image_tag() {
    local tag="$1"
    local temp_file

    temp_file="$(mktemp)"

    awk -v tag="$tag" '
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
    ' "$ENV_FILE" > "$temp_file"

    chmod --reference="$ENV_FILE" "$temp_file"
    mv "$temp_file" "$ENV_FILE"
}

wait_for_health() {
    local expected_tag="$1"
    local retries="${2:-$HEALTH_RETRIES}"
    local interval="${3:-$HEALTH_INTERVAL}"

    echo "Waiting for application health..."

    for (( attempt=1; attempt<=retries; attempt++ )); do
        backend_image="$(
            docker inspect -f '{{.Config.Image}}' retreat-backend 2>/dev/null || true
        )"

        web_image="$(
            docker inspect -f '{{.Config.Image}}' caddy 2>/dev/null || true
        )"

        if [[ "$backend_image" == "$BACKEND_IMAGE:$expected_tag" ]] \
            && [[ "$web_image" == "$WEB_IMAGE:$expected_tag" ]]; then

            health_response="$(
                curl -fsS \
                    --connect-timeout 3 \
                    --max-time 5 \
                    --resolve "$SITE_HOST:443:127.0.0.1" \
                    "https://$SITE_HOST/api/health" \
                    2>/dev/null || true
            )"

            if grep -q '"status":"UP"' <<< "$health_response"; then
                echo "Health check passed."
                return 0
            fi
        fi

        echo "Health check $attempt/$HEALTH_RETRIES failed; retrying..."
        sleep "$interval"
    done

    return 1
}

rollback() {
    local previous_tag="$1"

    if [[ -z "$previous_tag" ]]; then
        echo "ERROR: no previous deployment available for rollback" >&2
        return 1
    fi

    echo
    echo "Deployment failed."
    echo "Rolling back to:"
    echo "  $previous_tag"

    set_image_tag "$previous_tag"

    cd "$SERVER_DIR"

    if ! docker compose up -d --no-deps backend caddy; then
        echo "CRITICAL: rollback container startup failed" >&2
        return 1
    fi

    if ! wait_for_health \
        "$previous_tag" \
        "$ROLLBACK_HEALTH_RETRIES" \
        "$ROLLBACK_HEALTH_INTERVAL"; then
        echo "CRITICAL: rollback health check failed" >&2
        return 1
    fi

    echo
    echo "Rollback completed successfully."
}

echo "Current: ${CURRENT_TAG:-none}"
echo "Target : $TAG"

echo
echo "Pulling deployment images..."

docker pull "$BACKEND_IMAGE:$TAG"
docker pull "$WEB_IMAGE:$TAG"

if [[ -n "$CURRENT_TAG" ]]; then
    printf '%s\n' "$CURRENT_TAG" > "$SERVER_DIR/.previous-image-tag"
    chmod 600 "$SERVER_DIR/.previous-image-tag"
fi

set_image_tag "$TAG"

cd "$SERVER_DIR"

echo
echo "Starting deployment..."

if ! docker compose up -d --no-deps backend caddy; then
    rollback "$CURRENT_TAG"
    exit 1
fi

if ! wait_for_health "$TAG"; then
    rollback "$CURRENT_TAG"
    exit 1
fi

echo
docker compose ps

echo
echo "Deployment completed:"
echo "  $TAG"
