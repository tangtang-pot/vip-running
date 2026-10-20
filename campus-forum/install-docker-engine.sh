#!/usr/bin/env bash
set -euo pipefail
export DEBIAN_FRONTEND=noninteractive
apt-get update
# Ubuntu's signed distribution packages: the download.docker.com host is
# unreachable on this machine. Never disable APT signature verification.
apt-get install -y --no-install-recommends ca-certificates curl iptables docker.io docker-compose-v2
update-alternatives --set iptables /usr/sbin/iptables-legacy
update-alternatives --set ip6tables /usr/sbin/ip6tables-legacy
# Private WSL distro root disk resides at G:\DockerWSL; do not expose a TCP API.
if ! docker info >/dev/null 2>&1; then
  nohup dockerd --host=unix:///var/run/docker.sock > /var/log/docker-campus.log 2>&1 < /dev/null &
fi
for attempt in $(seq 1 40); do
  if docker info >/dev/null 2>&1; then
    docker version
    docker info --format '{{.DockerRootDir}}'
    docker compose version
    exit 0
  fi
  sleep 1
done
tail -n 60 /var/log/docker-campus.log
exit 1
