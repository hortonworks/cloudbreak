#!/usr/bin/env bash

set -e

#
# Add anonymous access only for replication agreements
#
export HOSTCN=`hostname -d| sed -E 's/^|\./,dc=/g' | sed 's/^,//g'`

ldapmodify -x -D "cn=directory manager" -w $FPW  -h $HOSTNAME << EOF
dn: cn="$HOSTCN",cn=mapping tree,cn=config
changetype: modify
add: aci
aci: (targetattr=*)(targetfilter="(|(objectclass=nsds5replicationagreement)(objectclass=nsDSWindowsReplicationAgreement))")(version 3.0; aci "permission:Read Replication Agreements"; allow (read, search, compare) groupdn = "ldap:///anyone";)
EOF
