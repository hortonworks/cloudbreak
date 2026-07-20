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

# On a retry after a prior attempt already completed ipa-replica-install, skip the entire
# destructive reinstall (uninstall -> pre-join cleanup gate -> client/replica install) and only
# re-verify replication health in place. Reinstalling would tear down a working replica and leave a
# topology-plugin-managed agreement on the peer that raw ldapdelete cannot remove (err=53),
# dead-locking the cleanup gate. The marker is written only after ipa-replica-install below
# succeeds, so a failed install still triggers a full destructive retry.
if [ -f /var/log/freeipa_replica_install_completed ]; then
  echo "Prior attempt already completed ipa-replica-install for $FQDN; skipping reinstall and re-verifying replication health in place"
else

ipa-server-install --unattended --uninstall --ignore-topology-disconnect --ignore-last-of-role

# A failed prior attempt can leave dirsrv's GSSAPI credential cache (owned by the dirsrv user, uid
# 389 -> /tmp/krb5cc_389) latched onto a stale/expired ticket. The freshly installed dirsrv would
# reuse that cache and its outbound replication binds would fail silently. dirsrv is stopped by the
# uninstall above, so remove the cache now to force the next dirsrv to rebuild credentials from its
# keytab.
rm -f /tmp/krb5cc_389

if [ -x /opt/salt/scripts/freeipa_check_replication_cleanup.sh ]; then
  echo "Waiting for FreeIPA replication cleanup on peer $FREEIPA_TO_REPLICATE before re-joining as $FQDN"
  # Non-fatal on purpose. The cleanup script can only ldapdelete un-managed "meTo<host>" orphans;
  # if a prior partial install left a topology-plugin-managed agreement, ldapdelete is refused
  # (err=53) and the gate can never converge. That agreement is removed topology-correctly by the
  # "ipa server-del $FQDN" below (after re-enrollment), so this gate must NOT abort the re-join.
  # A short timeout keeps the common case (already-clean, or an un-managed orphan that deletes
  # immediately) fast without burning the full window on an entry only server-del can fix.
  if ! LDAP_URI="ldap://$FREEIPA_TO_REPLICATE" TARGET_HOSTS="$FQDN" FPW="$FPW" TIMEOUT_SECONDS=60 \
       /opt/salt/scripts/freeipa_check_replication_cleanup.sh; then
    echo "Pre-join replication cleanup did not fully converge (likely a topology-managed agreement); proceeding — 'ipa server-del $FQDN' below will remove it topology-correctly"
  fi
else
  echo "Replication cleanup check script not present (cluster predates the feature); skipping cleanup gate before re-joining as $FQDN"
fi

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
# hostname is set to FQDN for FreeIPA server. We need the short one, without domain here
HOSTNAME=$(hostname -s)
REVERSE_IP=$(echo "$IPADDR" | awk -F. '{print $4"."$3"." $2"."$1}')

echo "Check A record for ${HOSTNAME}"
if ! ipa dnsrecord-find {{ pillar['freeipa']['domain'] }}. "--name=${HOSTNAME}" "--a-rec=${IPADDR}" --all; then
  echo "Missing A record for ${HOSTNAME} with ${IPADDR}. Adding..."
  ipa dnsrecord-add {{ pillar['freeipa']['domain'] }}. "${HOSTNAME}" "--a-rec=${IPADDR}"
fi

if ! ipa dnsrecord-find {{ pillar['freeipa']['domain'] }}. "--name=${HOSTNAME}" "--a-rec=${IPADDR}" --all; then
  echo "Failed to set DNS A-record for ${HOSTNAME}"
  false
fi

for zone in $(ipa dnszone-find --raw | grep "idnsname:.*\.in-addr\.arpa\." | cut -d':' -f2 | awk '{ print length, $0 }' | sort -n -r | awk '{ print $2 }' | xargs)
do
    ZONE_NET=${zone//.in-addr.arpa./}
    if echo "$REVERSE_IP" | grep -qE "\.$ZONE_NET$"; then
        REVERSE_RECORD_NAME=$(echo "$REVERSE_IP" | sed "s/\.$ZONE_NET$//g")
        # dnsrecord-add must either add the record or modify it
        if ! ipa dnsrecord-find "$zone" "--name=$REVERSE_RECORD_NAME" "--ptr-rec=${FQDN}."; then
          echo "Missing PTR record for ${FQDN}, creating ${REVERSE_RECORD_NAME}"
          ipa dnsrecord-add "$zone" "$REVERSE_RECORD_NAME" "--ptr-rec=${FQDN}."
        fi
        if ipa dnsrecord-find "$zone" "--name=$REVERSE_RECORD_NAME" "--ptr-rec=${FQDN}."; then
          echo "PTR record for ${FQDN} with ${REVERSE_RECORD_NAME} already exists"
          break
        else
          echo "Failed to set Reverse DNS PTR-record for ${FQDN}"
          false
        fi
    fi
done

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

if [[ "${FORWARDERS}" == *" 169.254."* ]]; then
  echo "IPA does not work with link-local IP addresses, so not using it as the forwarder"
  FORWARDERS="--forwarder $FREEIPA_TO_REPLICATE_IP --auto-forwarders "
  cp /etc/resolv.conf.orig /etc/resolv.conf
fi

echo "Forwarders to use: [$FORWARDERS]"

ipa-replica-install \
          --setup-ca \
          --principal "$ADMIN_USER" \
          --admin-password "$FPW" \
          --setup-dns \
          --auto-reverse \
{%- if salt['pillar.get']('freeipa:reverseZones') %}
  {%- for zone in salt['pillar.get']('freeipa:reverseZones').split(',') %}
          --reverse-zone {{ zone }} \
  {%- endfor %}
{%- endif %}
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
{%- if grains['os_family'] == 'RedHat' and grains['osmajorrelease'] | int == 7 %}
          --no-ntp \
{%- endif %}
          --dirsrv-config-file /opt/salt/initial-ldap-conf.ldif

# Mark the replica install as completed so a subsequent retry (e.g. if the health gate below does
# not converge within its window) re-verifies in place instead of reinstalling.
echo "$(date +%Y-%m-%d:%H:%M:%S)" > /var/log/freeipa_replica_install_completed
fi

{%- if grains['os_family'] == 'RedHat' and grains['osmajorrelease'] | int >= 8 %}
echo "Verifying FreeIPA replication health after replica install"
if [ -x /opt/salt/scripts/freeipa_verify_replica_health.sh ]; then
  if ! /opt/salt/scripts/freeipa_verify_replica_health.sh; then
    echo "Replication health gate failed; breaking stale dirsrv ccache latch and restarting the IPA stack"
    # A plain 'ipactl restart' re-reads the poisoned external ccache. Stop the
    # whole stack, delete the stale ccache while dirsrv is down (forcing it to
    # rebuild from its keytab), then start everything back in dependency order
    # so the KDC/CA come up clean before dirsrv attempts GSSAPI replication.
    ipactl stop || true
    rm -f /tmp/krb5cc_389
    ipactl start || true
    if ! /opt/salt/scripts/freeipa_verify_replica_health.sh; then
      echo "Replication still unhealthy after ccache latch break and ipactl restart — replica install cannot proceed"
      exit 1
    fi
  fi
  echo "FreeIPA replication health verified successfully"
else
  echo "Replication health check script not present (cluster predates the feature); skipping replication health gate"
fi
{%- endif %}

set +e

# Id range initialization is after 'set +e' to not fail the state. FreeIPA installation/repair shouldn't fail because of this.
echo "Try to initialize DNA ID range on replica"
ipa -e in_server=True console /opt/salt/scripts/initdnarange.py
echo "Finished initializing DNA ID range on replica"
