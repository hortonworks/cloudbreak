#!/usr/bin/env bash

# Makes sure an installed FreeIPA server resolves through its own DNS server, as configured by
# "ipa-*-install --setup-dns" in its "changing resolv.conf to point to ourselves" step. IPA stores
# that resolver as NetworkManager global DNS in zzz-ipa.conf, but NetworkManager only renders
# /etc/resolv.conf on its own DNS updates (reload, restart, connection or lease changes) and does
# not notice a plain overwrite of the file. A replica whose resolv.conf was overwritten after the
# install would keep resolving through the seed server alone, and IPA DNS checks then time out
# once the seed is removed. This rewrites resolv.conf with the nameservers and search domains
# NetworkManager renders from zzz-ipa.conf.
#
# With --check it changes nothing and exits non-zero only when resolv.conf has to be rewritten.

set -e

NM_IPA_CONF=/etc/NetworkManager/conf.d/zzz-ipa.conf
RESOLV_CONF=/etc/resolv.conf

if [ ! -f "$NM_IPA_CONF" ]; then
  echo "$NM_IPA_CONF is not present, IPA did not configure the resolver through NetworkManager; leaving $RESOLV_CONF unchanged"
  exit 0
fi

# NetworkManager keyfile lists are separated by ',' or ';'
SERVERS=$(sed -n 's/^[[:space:]]*servers[[:space:]]*=//p' "$NM_IPA_CONF" | tr ',;' '  ' | xargs)
SEARCHES=$(sed -n 's/^[[:space:]]*searches[[:space:]]*=//p' "$NM_IPA_CONF" | tr ',;' '  ' | xargs)

if [ -z "$SERVERS" ]; then
  echo "No servers configured in $NM_IPA_CONF; leaving $RESOLV_CONF unchanged"
  exit 0
fi

CURRENT_SERVERS=$(grep -E '^[[:space:]]*nameserver[[:space:]]' "$RESOLV_CONF" 2>/dev/null | awk '{print $2}' | xargs || true)

if [ "$CURRENT_SERVERS" == "$SERVERS" ]; then
  echo "$RESOLV_CONF already resolves through the IPA DNS server(s): $SERVERS"
  exit 0
fi

if [ "$1" == "--check" ]; then
  echo "$RESOLV_CONF resolves through [$CURRENT_SERVERS] instead of the IPA DNS server(s) [$SERVERS] configured in $NM_IPA_CONF"
  exit 1
fi

echo "$RESOLV_CONF resolves through [$CURRENT_SERVERS]; pointing it back to the IPA DNS server(s) [$SERVERS] configured in $NM_IPA_CONF"
RESOLV_CONF_TMP=$(mktemp)
trap 'rm -f "$RESOLV_CONF_TMP"' EXIT
{
  echo "# Written by freeipa_ensure_local_resolver.sh from $NM_IPA_CONF"
  if [ -n "$SEARCHES" ]; then
    echo "search $SEARCHES"
  fi
  for server in $SERVERS; do
    echo "nameserver $server"
  done
} > "$RESOLV_CONF_TMP"
install -m644 "$RESOLV_CONF_TMP" "$RESOLV_CONF"
# The file is created from /tmp, so reset its SELinux label to the one policy expects for resolv.conf
restorecon -F "$RESOLV_CONF" 2>/dev/null || true
cat "$RESOLV_CONF"
