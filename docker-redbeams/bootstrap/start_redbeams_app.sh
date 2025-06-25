#!/bin/sh

set -e

: ${SECURE_RANDOM:=true}
: ${EXPOSE_JMX_METRICS:=false}
: ${EXPOSE_JMX_METRICS_PORT:=20105}
: ${EXPOSE_JMX_METRICS_CONFIG:=config.yaml}
: ${TRUSTED_CERT_DIR:=/certs/trusted}
: ${EXPOSE_JMX_BIND_ADDRESS:=0.0.0.0}
: ${SERVICE_SPECIFIC_CERT_DIR:=/redbeams/certs}
: ${CRYPTOSENSE_ENABLED:=false}

echo "Importing certificates to the default Java certificate  trust store."

import_cert_with_alias_to_trust_store() {
  cert_dir=$1
  cert=$2

  echo "Adding certificate from file $cert_dir/$cert to trust store"
  if keytool -import -alias "$cert" -noprompt -file "$cert_dir/$cert" -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit; then
      echo "Certificate added to default Java trust store with alias $cert."
  else
      echo "WARNING: Failed to add $cert to trust store."
  fi
}

import_certs_from_dir_to_keystore() {
  cert_dir_param=$1

  if [ -d "$cert_dir_param" ]; then
      echo "Starting to process certificates in $cert_dir_param directory."
      for cert in $(ls -A "$cert_dir_param"); do
          echo "checking file $cert_dir_param/$cert"
          if [ -f "$cert_dir_param/$cert" ]; then
              echo "It is a file checking for number of certificate entries $cert_dir_param/$cert"
              number_of_certs=$(grep -c 'END CERTIFICATE' "$cert_dir_param/$cert" || true)
              if [ "$number_of_certs" -gt 1 ]; then
                  echo "Splitting $cert_dir_param/$cert into multiple certificate files as it contains $number_of_certs certificates"
                  cert_bundle_dir="/${cert%.pem}/"
                  mkdir -p "$cert_bundle_dir"
                  awk 'split_after==1{n++;split_after=0} /-----END CERTIFICATE-----/ {split_after=1} {print > "'"${cert_bundle_dir}"'cert" n ".pem"}' "$cert_dir_param/$cert"
                  for certbundle_part in $(ls -A "$cert_bundle_dir"); do
                      import_cert_with_alias_to_trust_store "$cert_bundle_dir" "$certbundle_part"
                  done;
              else
                  echo "import single cert from single file $cert_dir_param/$cert"
                  import_cert_with_alias_to_trust_store "$cert_dir_param" "$cert"
              fi
          else
            echo "it is not file: $cert_dir_param/$cert"
          fi
      done
  else
      echo "NOT an existing directory $cert_dir_param"
  fi
}

import_certs_from_dir_to_keystore $TRUSTED_CERT_DIR
import_certs_from_dir_to_keystore $SERVICE_SPECIFIC_CERT_DIR

echo "Starting the Redbeams application..."

REDBEAMS_JAVA_OPTS="$REDBEAMS_JAVA_OPTS -XX:+ExitOnOutOfMemoryError"

set -x
if [ "$SECURE_RANDOM" == "false" ]; then
  REDBEAMS_JAVA_OPTS="$REDBEAMS_JAVA_OPTS -Djava.security.egd=file:/dev/./urandom"
fi

if [ "$CRYPTOSENSE_ENABLED" == "true" ]; then
  REDBEAMS_JAVA_OPTS="$REDBEAMS_JAVA_OPTS -Dcryptosense.agent.out=/cryptosense/logs -javaagent:/cryptosense/cs-java-tracer.jar"
fi

# If presence of bouncycastle-fips in an image is detected, do not set any other security options (neither FIPS or non-fips).
BC_FIPS_LOCATION="/usr/share/java/bouncycastle-fips"
if [ -e "${BC_FIPS_LOCATION}" ]; then
  SECURITY_OPTS="--module-path=${BC_FIPS_LOCATION}"
  if [ "${FIPS_MODE_ENABLED:-false}" = false ]; then
    SECURITY_OPTS="${SECURITY_OPTS} -Djava.security.properties=$JAVA_HOME/conf/security/java-nonfips.security"
  fi
  echo "env variable 'JDK_JAVA_FIPS_OPTIONS='${JDK_JAVA_FIPS_OPTIONS} and 'JAVA_TRUSTSTORE_OPTIONS='${JAVA_TRUSTSTORE_OPTIONS} "
  SECURITY_OPTS="${SECURITY_OPTS} ${JDK_JAVA_FIPS_OPTIONS}"

  SECURITY_OPTS="${SECURITY_OPTS} --add-exports java.base/sun.security.ssl=ALL-UNNAMED"
  SECURITY_OPTS="${SECURITY_OPTS} --add-exports java.base/com.sun.crypto.provider=ALL-UNNAMED"
  SECURITY_OPTS="${SECURITY_OPTS} --add-exports org.bouncycastle.fips.core/org.bouncycastle.asn1.cryptlib=ALL-UNNAMED"
  SECURITY_OPTS="${SECURITY_OPTS} --add-exports org.bouncycastle.fips.core/org.bouncycastle.asn1.isara=ALL-UNNAMED"
  SECURITY_OPTS="${SECURITY_OPTS} --add-exports org.bouncycastle.fips.core/org.bouncycastle.math.ec.custom.gm=ALL-UNNAMED"
  SECURITY_OPTS="${SECURITY_OPTS} --add-exports org.bouncycastle.fips.core/org.bouncycastle.math.ec.custom.djb=ALL-UNNAMED"
else
  SECURITY_OPTS="${SECURITY_OPTS} -Djavax.net.ssl.keyStore=NONE -Djavax.net.ssl.keyStoreType=PKCS11 -Djavax.net.ssl.trustStore=NONE -Djavax.net.ssl.trustStoreType=PKCS11"
fi
REDBEAMS_JAVA_OPTS="${REDBEAMS_JAVA_OPTS} ${SECURITY_OPTS}"

eval "(java $REDBEAMS_JAVA_OPTS -jar /redbeams.jar) & JAVAPID=\$!; trap \"kill \$JAVAPID; wait \$JAVAPID\" SIGINT SIGTERM; wait \$JAVAPID"
