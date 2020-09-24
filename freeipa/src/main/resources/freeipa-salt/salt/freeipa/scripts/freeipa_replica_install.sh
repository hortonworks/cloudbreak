#!/usr/bin/env bash

set -e

FQDN=$(hostname -f)
IPADDR=$(hostname -i)

if [ ! -f /etc/resolv.conf.orig ]; then
  cp /etc/resolv.conf /etc/resolv.conf.orig
fi
FORWARDERS=$(grep -Ev '^#|^;' /etc/resolv.conf.orig | grep nameserver | awk '{print "--forwarder " $2}');

install -m644 /etc/resolv.conf.install /etc/resolv.conf

ipa-server-install --unattended --uninstall --ignore-topology-disconnect --ignore-last-of-role

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
          $FORWARDERS \
          --force-join \
{%- if not salt['pillar.get']('freeipa:dnssecValidationEnabled') %}
          --no-dnssec-validation \
{%- endif %}
          --unattended \
          --no-ntp

set +e
