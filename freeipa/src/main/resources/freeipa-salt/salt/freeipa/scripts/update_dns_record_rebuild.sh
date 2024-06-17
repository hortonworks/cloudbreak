#!/bin/bash

set -e

function cleanup() {
  kdestroy
}

trap cleanup EXIT

echo "$(date +'%Y-%m-%d %H:%M:%S') Starting DNS record update for rebuild"

IPA_FDOMAIN=$(hostname -d).
IPA_FQDN=$(hostname -f)
IPA_SHORT_HOSTNAME=$(echo "$IPA_FQDN" | cut -f1 -d".")
IPA_IP=$(hostname -i)
REVERSE_IP=$(echo "$IPA_IP" | awk -F. '{print $4"."$3"." $2"."$1}')

echo "IPA_FDOMAIN=$IPA_FDOMAIN"
echo "IPA_FQDN=$IPA_FQDN"
echo "IPA_SHORT_HOSTNAME=$IPA_SHORT_HOSTNAME"
echo "IPA_IP=$IPA_IP"
echo "REVERSE_IP=$REVERSE_IP"

echo "$ADMIN_PASSWORD" | kinit admin

echo "$(date +'%Y-%m-%d %H:%M:%S') update reverse DNS record"
ipa dnszone-find --pkey-only | grep "Zone name:" | awk '{print $3}' | while read -r DNS_ZONE; do
    set +e
    ipa dnsrecord-find "$DNS_ZONE" "--ptr-rec=$IPA_FQDN." &> /dev/null
    LAST=$?
    set -e
    if [ $LAST -eq 0 ];
    then
      echo "Found reverse DNS zone $DNS_ZONE for $IPA_IP"
      REC_NAME=$(ipa dnsrecord-find "$DNS_ZONE" "--ptr-rec=$IPA_FQDN." | grep "Record name:" | awk '{print $3}')
      ipa dnsrecord-del "$DNS_ZONE" "$REC_NAME" "--ptr-rec=$IPA_FQDN."
      ZONE_NET=${DNS_ZONE//.in-addr.arpa./}
      REVERSE_RECORD_NAME=$(echo "$REVERSE_IP" | sed "s/\.$ZONE_NET$//g")
      ipa dnsrecord-add "$DNS_ZONE" "$REVERSE_RECORD_NAME" "--ptr-rec=${IPA_FQDN}."
      break
    fi
done

echo "$(date +'%Y-%m-%d %H:%M:%S') update DNS A record"
ipa dnsrecord-mod $IPA_FDOMAIN $IPA_SHORT_HOSTNAME --a-rec=$IPA_IP

echo "$(date +'%Y-%m-%d %H:%M:%S') update system records"
ipa dns-update-system-records

echo "$(date +'%Y-%m-%d %H:%M:%S') finished"