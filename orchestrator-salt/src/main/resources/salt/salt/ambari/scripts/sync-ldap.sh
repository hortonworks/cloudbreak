#!/bin/bash

set -ex

ambari-server sync-ldap --all --ldap-sync-admin-name "{{ ambari.username }}" --ldap-sync-admin-password "{{ ambari.password }}"

echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/ldap_sync_success