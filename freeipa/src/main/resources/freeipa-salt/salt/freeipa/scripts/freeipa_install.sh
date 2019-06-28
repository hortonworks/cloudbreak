#!/usr/bin/env bash

set -e

FQDN=$(hostname -f)
IPADDR=$(hostname -i)

ipa-server-install \
          --realm $REALM \
          --domain $DOMAIN \
          --hostname $FQDN \
          -a $FPW \
          -p $FPW \
          --setup-dns \
          --auto-reverse \
          --allow-zone-overlap \
          --ssh-trust-dns \
          --mkhomedir \
          --ip-address $IPADDR \
          --auto-forwarders \
          --unattended

set +e