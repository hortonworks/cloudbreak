#!/bin/bash

# This script will help repair freeipa by cleaning up after restoring from a backup

set -e

ADMIN_PASSWORD=$(grep -v "^#" /srv/pillar/freeipa/init.sls | jq  '.freeipa.password' -r)
IPA_FDOMAIN=$(domainname).
IPA_FQDN=$(hostname -f)
IPA_SHORT_HOSTNAME=$(echo "$IPA_FQDN" | cut -f1 -d".")
IPA_IP=$(hostname -i)
IPA_SERVERS_LIST_FILE=/tmp/ipa_servers

function banner() {
  echo
  echo "<><><><><><><><><><><><><><><><><><><><><><>"
  echo "$1"
  echo
}

banner "Configuration"
echo "IPA_FDOMAIN=$IPA_FDOMAIN"
echo "IPA_FQDN=$IPA_FQDN"
echo "IPA_SHORT_HOSTNAME=$IPA_SHORT_HOSTNAME"
echo "IPA_IP=$IPA_IP"
echo "IPA_SERVERS_LIST_FILE=$IPA_SERVERS_LIST_FILE"

echo "$ADMIN_PASSWORD" | kinit admin

# Save a list of all the other FreeIPA servers so that they are saved in case the script must be run multiple times
if [ ! -f "$IPA_SERVERS_LIST_FILE" ]; then
  echo Saving a list of all FreeIPA servers to "$IPA_SERVERS_LIST_FILE"
  ipa server-find | grep "Server name:" | awk '{print $3}' > "$IPA_SERVERS_LIST_FILE"
else
  echo Using saved list of all FreeIPA servers from "$IPA_SERVERS_LIST_FILE"
fi

echo List of all IPA servers:
cat "$IPA_SERVERS_LIST_FILE"

banner "Deleting other FreeIPA servers"
while read -r IPA_SERVER_TO_CLEANUP; do
  if [ "$IPA_SERVER_TO_CLEANUP" != "$IPA_FQDN" ]; then
    echo Deleting the IPA server "$IPA_SERVER_TO_CLEANUP"
    ipa server-del --ignore-topology-disconnect --ignore-last-of-role --force "$IPA_SERVER_TO_CLEANUP"
  fi
done <$IPA_SERVERS_LIST_FILE


banner "Removing all FreeIPA servers from all reverse DNS zones"
ipa dnszone-find | grep "Zone name:" | awk '{print $3}' | while read -r DNS_ZONE; do
  while read -r IPA_SERVER_TO_CLEANUP; do
    set +e
    ipa dnsrecord-find "$DNS_ZONE" "--ptr-rec=$IPA_SERVER_TO_CLEANUP." &> /dev/null
    LAST=$?
    set -e
    if [ $LAST -eq 0 ];
    then
      echo "Removing $IPA_SERVER_TO_CLEANUP from DNS zone $DNS_ZONE"
      REC_NAME=$(ipa dnsrecord-find "$DNS_ZONE" "--ptr-rec=$IPA_SERVER_TO_CLEANUP." | grep "Record name:" | awk '{print $3}')
      ipa dnsrecord-del "$DNS_ZONE" "$REC_NAME" "--ptr-rec=$IPA_SERVER_TO_CLEANUP."
    fi
  done <$IPA_SERVERS_LIST_FILE
done


banner "Removing other FreeIPA servers from the forward DNS zone"
echo "Cleaning DNS zone $IPA_FDOMAIN"
while read -r IPA_SERVER_TO_CLEANUP; do
  if [ "$IPA_SERVER_TO_CLEANUP" != "$IPA_FQDN" ]; then
    SHORT_HOSTNAME=$(echo "$IPA_SERVER_TO_CLEANUP" | cut -f1 -d".")
    set +e
    ipa dnsrecord-show "$IPA_FDOMAIN" "$SHORT_HOSTNAME" &> /dev/null
    LAST=$?
    set -e
    if [ $LAST -eq 0 ];
    then
      echo Removing forward DNS record for "$SHORT_HOSTNAME" from "$IPA_FDOMAIN"
      ipa dnsrecord-del --del-all "$IPA_FDOMAIN" "$SHORT_HOSTNAME"
    fi
  fi
done <$IPA_SERVERS_LIST_FILE

banner "Fixing SOA records to reference $IPA_FQDN for all DNS zones"
ipa dnszone-find | grep "Zone name:" | awk '{print $3}' | while read -r DNS_ZONE; do
  echo "Fixing SOA record in for $DNS_ZONE"
  set +e
  ipa dnszone-mod "$DNS_ZONE" "--name-server=$IPA_FQDN."
  set -e
done

banner "Update forward DNS record"
echo "Fixing $IPA_SHORT_HOSTNAME record in DNS zone $IPA_FDOMAIN"
set +e
ipa dnsrecord-mod "$IPA_FDOMAIN" "$IPA_SHORT_HOSTNAME" "--a-rec=$IPA_IP"
set -e

banner "Updating IPS system records"
ipa dns-update-system-records

banner "Refreshing SSSD"
systemctl stop sssd
find /var/lib/sss/ ! -type d -print0 | xargs -0 rm -f
systemctl start sssd

banner "Complete"
