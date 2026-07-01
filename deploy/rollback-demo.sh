#!/usr/bin/env bash
set -euo pipefail

if [[ ${EUID} -ne 0 ]]; then
    echo "run as root" >&2
    exit 1
fi

systemctl disable --now church-retreat-ops.service 2>/dev/null || true
rm -f /etc/systemd/system/church-retreat-ops.service
rm -f /etc/church-retreat-ops/church-retreat-ops.env
rm -f /opt/church-retreat-ops/app.jar
rmdir /etc/church-retreat-ops /opt/church-retreat-ops 2>/dev/null || true
systemctl daemon-reload

echo "Spring demo service removed. Windows Caddy and PostgreSQL data were not removed."
