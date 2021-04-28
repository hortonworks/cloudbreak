#!/usr/bin/env bash

set -e

function cleanup() {
  kdestroy
}

trap cleanup EXIT

FQDN=$(hostname -f)
# Get the ipaddresses of the host
IPADDRS=$(hostname -i)
echo "The ipaddresses of the host are $IPADDRS"
# Get the first ipaddress. %% removes longest matching pattern in the end. ' *' pattern matches all but the first ipaddress.
IPADDR="${IPADDRS%% *}"
echo "The first ipaddress of the host is $IPADDR"

if [ ! -f /etc/resolv.conf.orig ]; then
  cp /etc/resolv.conf /etc/resolv.conf.orig
fi

install -m644 /etc/resolv.conf.install /etc/resolv.conf

ipa-server-install --unattended --uninstall --ignore-topology-disconnect --ignore-last-of-role

ipa-client-install \
  --server "$FREEIPA_TO_REPLICATE" \
  --realm "$REALM" \
  --domain "$DOMAIN" \
  --mkhomedir \
  --hostname "$FQDN" \
  --ip-address "$IPADDR" \
  --principal "$ADMIN_USER" \
  --password "$FPW" \
  --unattended \
  --force-join \
  --ssh-trust-dns \
  --no-ntp

echo "$FPW" | kinit $ADMIN_USER

if ipa server-find "$FQDN"; then
  echo "Cleaning up a prior installation for $FQDN. Deleting the server."
  ipa server-del --ignore-topology-disconnect --ignore-last-of-role --force "$FQDN"
fi

ipa topologysuffix-find | grep "Suffix name" | cut -f2 -d":" | cut -f2 -d" " | while read -r SUFFIX; do
  ipa topologysegment-find "--leftnode=$FQDN" "$SUFFIX" | grep "Segment name" | while read -r SEGMENT; do
    echo "Cleaning up a prior installation for $FQDN. Deleting the topology segment $SEGMENT for $SUFFIX."
    ipa topologysegment-del --continue "$SUFFIX" "$SEGMENT"
  done
  ipa topologysegment-find "--rightnode=$FQDN" "$SUFFIX" | grep "Segment name" | while read -r SEGMENT; do
    echo "Cleaning up a prior installation for $FQDN. Deleting the topology segment $SEGMENT for $SUFFIX."
    ipa topologysegment-del --continue "$SUFFIX" "$SEGMENT"
  done
done

if ipa hostgroup-show ipaservers | grep "$FQDN"; then
  echo "Cleaning up ipaservers host group for $FQDN"
  ipa hostgroup-remove-member ipaservers "--hosts=$FQDN"
fi

FORWARDERS=$(grep -Ev '^#|^;' /etc/resolv.conf.orig | grep nameserver | awk '{print "--forwarder " $2}')
PRIMARY_IPA=$(grep -Ev '^#|^;' /etc/resolv.conf | grep nameserver | awk '{print $2}')

if [[ "${FORWARDERS}" == *" 169.254."* ]]; then
  echo "IPA does not work with link-local IP addresses, so not using it as the forwarder"
  FORWARDERS="--forwarder $PRIMARY_IPA --auto-forwarders "
  cp /etc/resolv.conf.orig /etc/resolv.conf
fi

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
