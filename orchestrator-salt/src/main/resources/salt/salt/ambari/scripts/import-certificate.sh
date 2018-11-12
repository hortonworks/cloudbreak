#!/bin/bash

set -ex

mkdir -p {{ ldaps.keystorePath }}
$JAVA_HOME/bin/keytool -import -trustcacerts -alias ambari-ldaps -file {{ ldaps.certPath }} -keystore {{ ldaps.keystorePath }}/keystore.jks -storepass {{ ldaps.keystorePassword }} -noprompt

echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/import-certificate_success