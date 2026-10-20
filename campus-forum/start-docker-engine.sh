#!/usr/bin/env bash
set -euo pipefail
# The inbox WSL 5.10 kernel lacks nftables modules used by Ubuntu's default.
update-alternatives --set iptables /usr/sbin/iptables-legacy >/dev/null
update-alternatives --set ip6tables /usr/sbin/ip6tables-legacy >/dev/null
if ! docker info >/dev/null 2>&1; then
  nohup dockerd --host=unix:///var/run/docker.sock > /var/log/docker-campus.log 2>&1 < /dev/null &
fi
for attempt in $(seq 1 40); do
  if docker info >/dev/null 2>&1; then exit 0; fi
  sleep 1
done
tail -n 40 /var/log/docker-campus.log
exit 1
