#!/usr/bin/env bash

set -e

FQDN=$(hostname -f)
IPADDR=$(hostname -i)

install -m644 /etc/resolv.conf.install /etc/resolv.conf

ipa-server-install --unattended --uninstall

ipa-client-install \
  --server "$FREEIPA_TO_REPLICATE" \
  --realm "$REALM" \
  --domain "$DOMAIN" \
  --mkhomedir \
  --hostname $FQDN \
  --ip-address "$IPADDR" \
  --principal "$ADMIN_USER" \
  --password "$FPW" \
  --unattended \
  --force-join \
  --ssh-trust-dns \
  --no-ntp

ipa-replica-install \
          --setup-ca \
          --principal "$ADMIN_USER" \
          --admin-password "$FPW" \
          --setup-dns \
          --auto-reverse \
          --allow-zone-overlap \
          --ssh-trust-dns \
          --mkhomedir \
          --ip-address "$IPADDR" \
          --auto-forwarders \
          --force-join \
{%- if not salt['pillar.get']('freeipa:dnssecValidationEnabled') %}
          --no-dnssec-validation \
{%- endif %}
          --unattended \
          --no-ntp

set +e
