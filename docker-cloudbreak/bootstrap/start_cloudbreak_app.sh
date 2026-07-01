#!/bin/sh

set -e

: ${SECURE_RANDOM:=true}
: ${TRUSTED_CERT_DIR:=/certs/trusted}
: ${SERVICE_SPECIFIC_CERT_DIR:=/cloudbreak/certs}
: ${MOCK_INFRASTRUCTURE_CERT_DIR:=/certs/mock-infrastructure}
: ${CRYPTOSENSE_ENABLED:=false}

echo "Importing certificates to the default Java certificate trust store."

java /ImportCerts.java "$JAVA_HOME/lib/security/cacerts" changeit "$TRUSTED_CERT_DIR" "$SERVICE_SPECIFIC_CERT_DIR" "$MOCK_INFRASTRUCTURE_CERT_DIR"

JACOCO_AGENT_OPTIONS=""
if [ "${JACOCO_AGENT_ENABLED:-false}" = true ]; then
    : ${JACOCO_AGENT_DIR:=/tmp/jacoco}
    mkdir -p ${JACOCO_AGENT_DIR}
    : ${JACOCO_AGENT_VERSION:=0.8.13}
    : ${JACOCO_AGENT_PORT:=6300}
    JACOCO_AGENT_JAR_URL="https://nexus-private.eng.cloudera.com/nexus/repository/public/org/jacoco/org.jacoco.agent/${JACOCO_AGENT_VERSION}/org.jacoco.agent-${JACOCO_AGENT_VERSION}-runtime.jar"
    curl -fsSL -o "${JACOCO_AGENT_DIR}/jacocoagent.jar" "${JACOCO_AGENT_JAR_URL}"
    if [ ! -f "${JACOCO_AGENT_DIR}/jacocoagent.jar" ]; then
        echo "JACOCO agent not found. Please check the url: ${JACOCO_AGENT_JAR_URL} and path: ${JACOCO_AGENT_DIR}/jacocoagent.jar"
        exit 1
    fi
    JACOCO_AGENT_OPTIONS="-javaagent:${JACOCO_AGENT_DIR}/jacocoagent.jar=output=tcpserver,port=${JACOCO_AGENT_PORT},address=*"
fi

echo "Starting the Cloudbreak application..."

CB_JAVA_OPTS="$CB_JAVA_OPTS -XX:+ExitOnOutOfMemoryError"

set -x
if [ "$SECURE_RANDOM" == "false" ]; then
  CB_JAVA_OPTS="$CB_JAVA_OPTS -Djava.security.egd=file:/dev/./urandom"
fi

if [ "$CRYPTOSENSE_ENABLED" == "true" ]; then
  CB_JAVA_OPTS="$CB_JAVA_OPTS -Dcryptosense.agent.out=/cryptosense/logs -javaagent:/cryptosense/cs-java-tracer.jar"
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
CB_JAVA_OPTS="${CB_JAVA_OPTS} ${SECURITY_OPTS} ${JACOCO_AGENT_OPTIONS}"

eval "(java $CB_JAVA_OPTS -jar /cloudbreak.jar) & JAVAPID=\$!; trap \"kill \$JAVAPID; wait \$JAVAPID\" SIGINT SIGTERM; wait \$JAVAPID"
