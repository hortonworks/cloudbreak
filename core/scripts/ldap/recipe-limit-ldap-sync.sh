#!/bin/bash
# To limit the Number of LDAP users get's synced to ambari this script can be used as a recipe

SALT_LOCATION=$(ls -d /opt/salt_*)

source "$SALT_LOCATION/bin/activate"

AMBARI_PW=$(salt '*' pillar.item 'ambari:password' --out json | grep ambari.pass | awk -F'"' '{print $4}' | grep -v "^$")

echo "Applying the patch for ldap sync, creating users.txt and group.txt files"

# These 2 lines create the necessary files for groups and users and should be customized for the customer's LDAP.
echo "admin,admin2" >> $(pwd)/users.txt
echo "group1,group2" >> $(pwd)/groups.txt

echo "Patching /srv/salt/ambari/scripts/sync-ldap.sh to use users.txt and group.txt for sync"

cat <<EOF >/srv/salt/ambari/scripts/sync-ldap.sh
#!/bin/sh
set -ex

#patch ambari sync for specific set of users
ambari-server sync-ldap --users users.txt --groups groups.txt --ldap-sync-admin-name cloudbreak --ldap-sync-admin-password $AMBARI_PW

echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/ldap_sync_success
EOF
