#!/usr/bin/env bash

set -e

FQDN=$(hostname -f)
IPADDR=$(hostname -i)

ipa-replica-install \
          --server $FREEIPA_TO_REPLICATE \
          --setup-ca \
          --realm $REALM \
          --domain $DOMAIN \
          --hostname $FQDN \
          --principal $ADMIN_USER \
          --admin-password $FPW \
          --setup-dns \
          --auto-reverse \
          --allow-zone-overlap \
          --ssh-trust-dns \
          --mkhomedir \
          --ip-address $IPADDR \
          --auto-forwarders \
          --unattended

set +e
