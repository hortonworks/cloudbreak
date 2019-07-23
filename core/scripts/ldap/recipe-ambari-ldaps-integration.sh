#!/bin/bash

# To enable Ambari-LDAPS integration as a post-cloudera-manager-start recipe
PATH_TO_YOUR_LDAPS_CERT=/tmp/cert
AMBARI_USER=admin
AMBARI_PASS=Admin123

echo -n | openssl s_client -connect hwxmsad-bd87e95aa9775a71.elb.eu-west-1.amazonaws.com:636 | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > $PATH_TO_YOUR_LDAPS_CERT
mkdir /etc/ambari-server/keys
$JAVA_HOME/bin/keytool -import -trustcacerts -alias root -file $PATH_TO_YOUR_LDAPS_CERT -keystore /etc/ambari-server/keys/ldaps-keystore.jks -storepass mypass -noprompt

ambari-server setup-ldap \
--ldap-url="hwxmsad-bd87e95aa9775a71.elb.eu-west-1.amazonaws.com:636" \
--ldap-secondary-url="" \
--ldap-ssl="true" \
--ldap-user-class="person" \
--ldap-user-attr="sAMAccountName" \
--ldap-group-class="group" \
--ldap-group-attr="cn" \
--ldap-member-attr="member" \
--ldap-dn="distunguishedName" \
--ldap-base-dn="OU=Users,OU=AD,DC=AD,DC=HWX,DC=COM" \
--ldap-referral="" \
--ldap-bind-anonym=false \
--ldap-manager-dn="Admin@AD.HWX.COM" \
--ldap-manager-password="H>?[{8*GcSn<z*oM" \
--ldap-sync-username-collisions-behavior="skip" \
--ldap-save-settings \
--truststore-type="jks" \
--truststore-path="/etc/ambari-server/keys/ldaps-keystore.jks" \
--truststore-password="mypass"

service ambari-server restart
ambari-server sync-ldap --all --ldap-sync-admin-name=$AMBARI_USER --ldap-sync-admin-password=$AMBARI_PASS
