#!/usr/bin/env bash
# Name: freeipa_healthagent_setup
# Description: Setup the environment for the freeipa health agent to be used
################################################################
set -x
set -e

ESCAPED_DOMAIN=$(hostname -d| sed -E 's/^|\./\\,dc\\3D/g' | sed 's/^\\,//g')
REPLICA_BASE="cn=${ESCAPED_DOMAIN},cn=mapping tree,cn=config"

#
# Add anonymous access for replication agreements
#
set +ex
echo ldapmodify -x -D "cn=directory manager" -w "****" -h localhost
ldapmodify -x -D "cn=directory manager" -w "$FPW" -h localhost << EOF
dn: ${REPLICA_BASE}
changetype: modify
add: aci
aci: (targetattr="cn||objectClass||nsDS5ReplicaHost||nsds5replicaLastUpdateEnd||nsds5replicaLastUpdateStatus")(targetfilter="(|(objectclass=nsds5replicationagreement)(objectclass=nsDSWindowsReplicationAgreement))")(version 3.0; aci "permission:Read Replication Agreements"; allow (read, search, compare) groupdn = "ldap:///anyone";)
EOF
LDAPMODIFY_RET=$?
set -ex
LDAP_TYPE_OR_VALUE_EXISTS=20
if [[ $LDAPMODIFY_RET -ne 0 && $LDAPMODIFY_RET -ne $LDAP_TYPE_OR_VALUE_EXISTS ]]; then
  echo ldapmodify failed
  false
fi

#
# Setup cert copy and service refstart on cert update
#
ipa-getcert start-tracking -n Server-Cert -d /etc/httpd/alias -C "/usr/libexec/ipa/certmonger/restart_httpd;/cdp/ipahealthagent/freeipa_healthagent_getcerts.sh"
