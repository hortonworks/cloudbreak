#!/usr/bin/env bash

set -e

FQDN=$(hostname -f)
IPADDR=$(hostname -i)

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
          --ip-address $IPADDR \
          --auto-forwarders \
{%- if not salt['pillar.get']('freeipa:dnssecValidationEnabled') %}
          --no-dnssec-validation \
{%- endif %}
          --unattended \
          --no-ntp

set +e
