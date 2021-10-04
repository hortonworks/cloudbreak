#!/usr/bin/env bash

set +ex
echo ldapmodify -x -D "cn=directory manager" -w "****" -h localhost
ldapmodify -x -D "cn=directory manager" -w "$FPW" -h localhost << EOF
dn: cn=config
changetype: modify
replace: nsslapd-allow-anonymous-access
nsslapd-allow-anonymous-access: rootdse
EOF
LDAPMODIFY_RET=$?
set -ex
LDAP_TYPE_OR_VALUE_EXISTS=20
if [[ $LDAPMODIFY_RET -ne 0 && $LDAPMODIFY_RET -ne $LDAP_TYPE_OR_VALUE_EXISTS ]]; then
  echo ldapmodify failed
  false
fi
