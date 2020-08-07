#!/usr/bin/env bash

set -e

FQDN=$(hostname -f)
IPADDR=$(hostname -i)

if [ ! -f /etc/resolv.conf.orig ]; then
  cp /etc/resolv.conf /etc/resolv.conf.orig
fi
FORWARDERS=$(grep -Ev '^#|^;' /etc/resolv.conf.orig | grep nameserver | awk '{print "--forwarder " $2}');

ipa-server-install --unattended --uninstall

ipa-server-install \
          --realm "$REALM" \
          --domain "$DOMAIN" \
          --hostname "$FQDN" \
          -a "$FPW" \
          -p "$FPW" \
          --setup-dns \
          --auto-reverse \
{%- for zone in salt['pillar.get']('freeipa:reverseZones').split(',') %}
          --reverse-zone {{ zone }} \
{%- endfor %}
          --allow-zone-overlap \
          --ssh-trust-dns \
          --mkhomedir \
          --ip-address "$IPADDR" \
          $FORWARDERS \
{%- if not salt['pillar.get']('freeipa:dnssecValidationEnabled') %}
          --no-dnssec-validation \
{%- endif %}
          --unattended \
          --no-ntp

set +e
