#!/usr/bin/env bash

set -e

FQDN=$(hostname -f)
IPADDR=$(hostname -i)

ipa-server-install \
          --realm $REALM \
          --domain $DOMAIN \
          --hostname $FQDN \
          -a $FPW \
          -p $FPW \
          --setup-dns \
          --auto-reverse \
          --allow-zone-overlap \
          --ssh-trust-dns \
          --mkhomedir \
          --ip-address $IPADDR \
          --auto-forwarders \
          --unattended

export KRB5CCNAME=$(mktemp krb5cc.XXXXXX)
echo $FPW | kinit admin

# Allow anonymous (non-bound) LDAP users to search for
# group membership. This is a workaround for DWX-945 until
# we have a properly set up bind user/password.
basedn=$(ipa env basedn | awk '{print $2}')
ipa permission-add \
  --right=search \
  --attrs=member --attrs=gidNumber \
  --bindtype=anonymous \
  --filter='(|(objectclass=ipausergroup)(objectclass=posixgroup))' \
  --subtree=cn=groups,cn=accounts,$basedn \
  "Anonymous LDAP can search groups"
rm -f $KRB5CCNAME
unset KRB5CCNAME

set +e
