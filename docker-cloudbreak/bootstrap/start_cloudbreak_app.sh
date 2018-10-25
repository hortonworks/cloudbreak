#!/bin/bash

: ${SECURE_RANDOM:=true}
: ${EXPOSE_JMX_METRICS:=false}
: ${EXPOSE_JMX_METRICS_PORT:=20105}
: ${EXPOSE_JMX_METRICS_CONFIG:=config.yaml}
: ${TRUSTED_CERT_DIR:=/certs/trusted}
: ${EXPOSE_JMX_BIND_ADDRESS:=0.0.0.0}

echo "Importing certificates to the default Java certificate  trust store."

if [ -d "$TRUSTED_CERT_DIR" ]; then
    for cert in $(ls -A "$TRUSTED_CERT_DIR"); do
        if [ -f "$TRUSTED_CERT_DIR/$cert" ]; then
            if keytool -import -alias "$cert" -noprompt -file "$TRUSTED_CERT_DIR/$cert" -keystore /etc/ssl/certs/java/cacerts -storepass changeit; then
                echo -e "Certificate added to default Java trust store with alias $cert."
            else
                echo -e "WARNING: Failed to add $cert to trust store.\n"
            fi
        fi
    done
fi

echo "Starting the Cloudbreak application..."

set -x
if [ "$SECURE_RANDOM" == "false" ]; then
  CB_JAVA_OPTS="$CB_JAVA_OPTS -Djava.security.egd=file:/dev/./urandom"
fi
if [ "$EXPOSE_JMX_METRICS" == "true" ]; then
  CB_JAVA_OPTS="$CB_JAVA_OPTS -javaagent:/jmx_prometheus_javaagent.jar=$EXPOSE_JMX_BIND_ADDRESS:$EXPOSE_JMX_METRICS_PORT:$EXPOSE_JMX_METRICS_CONFIG"
fi

eval "(java $CB_JAVA_OPTS -jar /cloudbreak.jar) & JAVAPID=\$!; trap \"kill \$JAVAPID; wait \$JAVAPID\" SIGINT SIGTERM; wait \$JAVAPID"
