#!/usr/bin/env bash

set -e

function cleanup() {
  kdestroy
}

trap cleanup EXIT

FQDN=$(hostname -f)

echo "$FPW" | kinit $ADMIN_USER
CURRENT_CA_MASTER=`ipa config-show | grep "CA renewal master" | cut -d":" -f2 | cut -d" " -f2`
echo "The current CA renewal master is $CURRENT_CA_MASTER"
if [[ "$CURRENT_CA_MASTER" != "$FQDN" ]]; then
  ipa config-mod --ca-renewal-master-server $FQDN
  echo "The current CA renewal master was changed to $FQDN"
fi
ipa-crlgen-manage enable

set +e
