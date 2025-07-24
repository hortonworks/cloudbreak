#!/usr/bin/env bash

set -e

FQDN=$(hostname -f)
# Get the ipaddresses of the host
IPADDRS=$(hostname -i)
echo "The ipaddresses of the host are $IPADDRS"
# Get the first ipaddress. %% removes longest matching pattern in the end. ' *' pattern matches all but the first ipaddress.
IPADDR="${IPADDRS%% *}"
echo "The first ipaddress of the host is $IPADDR"

ipa-server-install --unattended --uninstall

ipa-server-install \
          --realm "$REALM" \
          --domain "$DOMAIN" \
          --hostname "$FQDN" \
          -a "$FPW" \
          -p "$FPW" \
          --setup-dns \
{%- if salt['pillar.get']('environmentType', 'PUBLIC_CLOUD') == 'PUBLIC_CLOUD' %}
          --auto-reverse \
{%- endif %}
{%- if salt['pillar.get']('freeipa:reverseZones') %}
  {%- for zone in salt['pillar.get']('freeipa:reverseZones').split(',') %}
          --reverse-zone {{ zone }} \
  {%- endfor %}
{%- endif %}
          --allow-zone-overlap \
          --ssh-trust-dns \
          --mkhomedir \
          --ip-address $IPADDR \
          --auto-forwarders \
{%- if not salt['pillar.get']('freeipa:dnssecValidationEnabled') %}
          --no-dnssec-validation \
{%- endif %}
          --unattended \
          --dirsrv-config-file /opt/salt/initial-ldap-conf.ldif \
          --no-ntp

set +e
