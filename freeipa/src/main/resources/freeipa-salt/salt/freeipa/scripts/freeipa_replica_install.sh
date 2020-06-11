#!/usr/bin/env bash

set -e

FQDN=$(hostname -f)
IPADDR=$(hostname -i)

ipa-server-install --unattended --uninstall

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
          --force-join \
{%- if not salt['pillar.get']('freeipa:dnssecValidationEnabled') %}
          --no-dnssec-validation \
{%- endif %}
          --unattended \
          --no-ntp

set +e
