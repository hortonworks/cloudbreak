#!/bin/bash

set -ex

    ambari-server setup-security \
      --security-option=setup-truststore \
      --truststore-path={{ ldaps.keystorePath }}/keystore.jks \
      --truststore-type=jks \
      --truststore-password={{ ldaps.keystorePassword }} \
      --truststore-reconfigure

echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/setup_truststore_success