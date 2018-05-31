#!/bin/bash

set -ex

# provide "n" for custom truststore in case we use ldaps.
yes n | ambari-server setup-ldap \
  --ldap-url="{{ ldap.serverHost }}:{{ ldap.serverPort }}" \
  --ldap-secondary-url="{{ ldap.serverHost }}:{{ ldap.serverPort }}" \
  --ldap-ssl="{{ ambari.secure_ldap }}" \
  --ldap-user-class="{{ ldap.userObjectClass }}" \
  --ldap-user-attr="{{ ldap.userNameAttribute }}" \
  --ldap-group-class="{{ ldap.groupObjectClass }}" \
  --ldap-group-attr="{{ ldap.groupNameAttribute }}" \
  --ldap-member-attr="{{ ldap.groupMemberAttribute }}" \
  --ldap-dn="distinguishName" \
  --ldap-base-dn="{{ ldap.userSearchBase }}" \
  --ldap-referral="follow" \
  --ldap-bind-anonym=false \
  --ldap-manager-dn="{{ ldap.bindDn }}" \
  --ldap-manager-password='{{ ldap.bindPassword }}' \
  --ldap-sync-username-collisions-behavior="convert" \
  --ldap-save-settings

echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/ldap_setup_success
