#!/bin/bash

set -e

: ${SECURE_RANDOM:=true}
: ${EXPOSE_JMX_METRICS:=false}
: ${EXPOSE_JMX_METRICS_PORT:=20105}
: ${EXPOSE_JMX_METRICS_CONFIG:=config.yaml}
: ${TRUSTED_CERT_DIR:=/certs/trusted}
: ${EXPOSE_JMX_BIND_ADDRESS:=0.0.0.0}
: ${SERVICE_SPECIFIC_CERT_DIR:=/consumption/certs}

echo "Importing certificates to the default Java certificate  trust store."

import_certs_from_dir_to_keystore() {
  cert_dir_param=$1

  if [ -d "$cert_dir_param" ]; then
      echo -e "Starting to process certificates in $cert_dir_param directory."
      for cert in $(ls -A "$cert_dir_param"); do
          if [ -f "$cert_dir_param/$cert" ]; then
              if keytool -import -alias "$cert" -noprompt -file "$cert_dir_param/$cert" -keystore /etc/pki/java/cacerts -storepass changeit; then
                  echo -e "Certificate added to default Java trust store with alias $cert."
              else
                  echo -e "WARNING: Failed to add $cert to trust store.\n"
              fi
          fi
      done
  else
      echo -e "NOT an existing directory $cert_dir_param"
  fi
}

import_certs_from_dir_to_keystore $TRUSTED_CERT_DIR
import_certs_from_dir_to_keystore $SERVICE_SPECIFIC_CERT_DIR

echo "Starting the Consumption application..."

set -x
if [ "$SECURE_RANDOM" == "false" ]; then
  CONSUMPTION_JAVA_OPTS="$CONSUMPTION_JAVA_OPTS -Djava.security.egd=file:/dev/./urandom"
fi

CONSUMPTION_JAVA_OPTS="$CONSUMPTION_JAVA_OPTS -Djavax.net.ssl.keyStore=NONE -Djavax.net.ssl.keyStoreType=PKCS11 -Djavax.net.ssl.trustStore=NONE -Djavax.net.ssl.trustStoreType=PKCS11"

eval "(java $CONSUMPTION_JAVA_OPTS -jar /consumption.jar) & JAVAPID=\$!; trap \"kill \$JAVAPID; wait \$JAVAPID\" SIGINT SIGTERM; wait \$JAVAPID"
