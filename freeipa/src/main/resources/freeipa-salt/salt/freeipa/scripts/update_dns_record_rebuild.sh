#!/bin/bash

set -e

function cleanup() {
  kdestroy
}

trap cleanup EXIT

function errcho(){ >&2 echo "$@"; }

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

echo "$(date +'%Y-%m-%d %H:%M:%S') delete old reverse DNS record"
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
      break
    fi
done

REVERSE_RECORD_CREATED=0
echo "$(date +'%Y-%m-%d %H:%M:%S') create reverse DNS record"
for zone in $(ipa dnszone-find --raw | grep "idnsname:.*\.in-addr\.arpa\." | cut -d':' -f2 | awk '{ print length, $0 }' | sort -n -r | awk '{ print $2 }' | xargs)
do
    ZONE_NET=${zone//.in-addr.arpa./}
    if echo "$REVERSE_IP" | grep -qE "\.$ZONE_NET$"; then
        REVERSE_RECORD_NAME=$(echo "$REVERSE_IP" | sed "s/\.$ZONE_NET$//g")
        # dnsrecord-add must either add the record or modify it
        if ! ipa dnsrecord-find "$zone" "--name=$REVERSE_RECORD_NAME" "--ptr-rec=${IPA_FQDN}."; then
          echo "Missing PTR record for ${IPA_FQDN}, creating ${REVERSE_RECORD_NAME}"
          ipa dnsrecord-add "$zone" "$REVERSE_RECORD_NAME" "--ptr-rec=${IPA_FQDN}."
        fi
        if ipa dnsrecord-find "$zone" "--name=$REVERSE_RECORD_NAME" "--ptr-rec=${IPA_FQDN}."; then
          echo "PTR record for ${IPA_FQDN} with ${REVERSE_RECORD_NAME} exists"
          REVERSE_RECORD_CREATED=1
          break
        else
          errcho "Failed to set Reverse DNS PTR-record for ${IPA_FQDN}"
          false
        fi
    fi
done

if [[ "$REVERSE_RECORD_CREATED" -eq 0 ]]; then
  errcho "Reverse record creation failed for $IPA_FQDN with IP $IPA_IP"
  false
fi

echo "$(date +'%Y-%m-%d %H:%M:%S') update DNS A record"
ipa dnsrecord-mod $IPA_FDOMAIN $IPA_SHORT_HOSTNAME --a-rec=$IPA_IP

echo "$(date +'%Y-%m-%d %H:%M:%S') update system records"
ipa dns-update-system-records

echo "$(date +'%Y-%m-%d %H:%M:%S') finished"