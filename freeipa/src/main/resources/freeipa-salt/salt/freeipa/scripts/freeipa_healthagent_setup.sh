#!/usr/bin/env bash

#
# Add anonymous access only for replication agreements
#

export HOSTCN=`hostname -d| sed -E 's/^|\./,dc=/g' | sed 's/^,//g'`

/opt/salt/scripts/freeipa_check_replication.sh
status=$?

if [[ status -ne 0 ]]; then
ldapmodify -x -D "cn=directory manager" -w $FPW -h localhost << EOF
dn: cn="$HOSTCN",cn=mapping tree,cn=config
changetype: modify
add: aci
aci: (targetattr=*)(targetfilter="(|(objectclass=nsds5replicationagreement)(objectclass=nsDSWindowsReplicationAgreement))")(version 3.0; aci "permission:Read Replication Agreements"; allow (read, search, compare) groupdn = "ldap:///anyone";)
EOF
fi

ipa-getcert start-tracking -n Server-Cert -d /etc/httpd/alias -C "/usr/libexec/ipa/certmonger/restart_httpd;/cdp/ipahealthagent/freeipa_healthagent_getcerts.sh"
