#!/bin/bash

function cleanup() {
  kdestroy
}

trap cleanup EXIT

echo {{salt['pillar.get']('sssd-ipa:password')}} | kinit {{salt['pillar.get']('sssd-ipa:principal')}}

set -x

ipa dnsrecord-find $(hostname -d) --name $(hostname) && ipa dnsrecord-del $(hostname -d) $(hostname) --del-all
ipa dnsrecord-find $(hostname -d) --a-rec $(hostname -i) && ipa dnsrecord-find $(hostname -d) --a-rec $(hostname -i) --raw | grep idnsname | cut -d':' -f2 | xargs -i ipa dnsrecord-del $(hostname -d) {} --del-all

for zone in `ipa dnszone-find --raw | grep idnsname | cut -d':' -f2 | xargs`
do
    ipa dnsrecord-find $zone --ptr-rec $(hostname -f). --raw && ipa dnsrecord-find $zone --ptr-rec $(hostname -f). --raw | grep idnsname | cut -d':' -f2 | xargs -i ipa dnsrecord-del $zone {} --del-all

    REVERSE_IP=$(hostname -i | awk -F. '{print $4"."$3"." $2"."$1}')
    ZONE_NET=$(echo $zone | sed 's/.in-addr.arpa.//g')
    if echo $REVERSE_IP | grep -qE "$ZONE_NET$"; then
        REVERSE_RECORD_NAME=$(echo $REVERSE_IP | sed "s/.$ZONE_NET$//g")
        ipa dnsrecord-find $zone --name $REVERSE_RECORD_NAME && ipa dnsrecord-del $zone $REVERSE_RECORD_NAME --del-all
    fi
done
