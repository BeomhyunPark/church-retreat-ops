#!/usr/bin/env bash
set -euo pipefail

if [[ ${EUID} -ne 0 ]]; then
    echo "run as root" >&2
    exit 1
fi

repo_dir=${1:?repository path is required}
jar_source="$repo_dir/build/libs/church-retreat-ops-0.0.1-SNAPSHOT.jar"
local_config="$repo_dir/src/main/resources/application-local.yml"
service_source="$repo_dir/deploy/church-retreat-ops.service"
environment_file=/etc/church-retreat-ops/church-retreat-ops.env

for path in /opt/church-retreat-ops/app.jar "$environment_file" /etc/systemd/system/church-retreat-ops.service; do
    if [[ -e "$path" ]]; then
        echo "refusing to overwrite existing deployment file: $path" >&2
        exit 1
    fi
done

db_url=$(awk '$1 == "url:" { print $2; exit }' "$local_config")
db_username=$(awk '$1 == "username:" { print $2; exit }' "$local_config")
db_password=$(awk '$1 == "password:" { print $2; exit }' "$local_config")

if [[ -z "$db_url" || -z "$db_username" || -z "$db_password" ]]; then
    echo "could not read demo database settings" >&2
    exit 1
fi

registration_count=$(docker compose -f "$repo_dir/docker-compose.yml" exec -T postgres \
    psql -U "$db_username" -d church_retreat_ops -tAc 'SELECT count(*) FROM registrations;')
registration_count=${registration_count//[[:space:]]/}

if [[ ! "$registration_count" =~ ^[0-9]+$ ]]; then
    echo "could not verify that the demo database is empty" >&2
    exit 1
fi

if (( registration_count != 0 )); then
    echo "refusing demo deployment because participant records already exist" >&2
    exit 1
fi

if ! id church-retreat-ops >/dev/null 2>&1; then
    useradd --system --home-dir /nonexistent --shell /usr/sbin/nologin church-retreat-ops
fi

install -d -m 0755 /opt/church-retreat-ops
install -d -m 0700 /etc/church-retreat-ops
install -m 0644 "$jar_source" /opt/church-retreat-ops/app.jar
install -m 0644 "$service_source" /etc/systemd/system/church-retreat-ops.service

umask 077
{
    printf 'APP_DB_URL=%s\n' "$db_url"
    printf 'APP_DB_USERNAME=%s\n' "$db_username"
    printf 'APP_DB_PASSWORD=%s\n' "$db_password"
    printf 'APP_JWT_SECRET=%s\n' "$(openssl rand -hex 48)"
    printf 'APP_SYSTEM_ADMIN_EMAIL=demo-admin@example.invalid\n'
    printf 'APP_SYSTEM_ADMIN_PASSWORD=%s\n' "$(openssl rand -hex 24)"
    printf 'APP_SYSTEM_ADMIN_NAME=Demo-Admin\n'
    printf 'APP_IDENTITY_APP_NAME=Retreat-Demo\n'
    printf 'APP_IDENTITY_ORGANIZATION_NAME=Demo-Only\n'
    printf 'APP_IDENTITY_EVENT_NAME=Production-like-Verification\n'
} > "$environment_file"

systemctl daemon-reload
systemctl enable --now church-retreat-ops.service
