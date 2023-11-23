FROM docker-private.infra.cloudera.com/cloudera_base/ubi8/cldr-openjdk-17-runtime-cis:1.18-2-cis-22112023
# We can not use alpine based image because of https://github.com/grpc/grpc-java/issues/8751
MAINTAINER info@cloudera.com

# REPO URL to download jar
ARG REPO_URL=https://nexus-private.hortonworks.com/nexus/content/groups/public
ARG VERSION=''

ENV VERSION ${VERSION}
ENV PROMETHEUS_VERSION 0.17.2

WORKDIR /

RUN microdnf install unzip

# install the freeipa app
ADD ${REPO_URL}/com/sequenceiq/freeipa/$VERSION/freeipa-$VERSION.jar /freeipa.jar

# add jmx exporter
RUN curl -o /jmx_prometheus_javaagent.jar https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/$PROMETHEUS_VERSION/jmx_prometheus_javaagent-$PROMETHEUS_VERSION.jar

# extract schema files
RUN ( unzip freeipa.jar schema/* -d / ) || \
    ( unzip freeipa.jar BOOT-INF/classes/schema/* -d /tmp/ && mv /tmp/BOOT-INF/classes/schema/ /schema/ )

# Install starter script for the FreeIpa application
COPY bootstrap/start_freeipa_app.sh /
COPY bootstrap/wait_for_freeipa_api.sh /

ENV JAVA_SECURITY_POLICY_FILE $JAVA_HOME/conf/security/java.security

# Add OpenSSL provider to Java security policy file (FIPS enabled mode only).
# - insert a line: fips.provider.1=OpenSSL
# - increase the index for other fips.provider lines.
RUN for I in {20..1}; do J=$((I+1)); sed -i "s/fips.provider.$I=/fips.provider.$J=/" $JAVA_SECURITY_POLICY_FILE; done \
&& N=$(grep -m 1 -n "^fips\.provider\.[[:digit:]]=" $JAVA_SECURITY_POLICY_FILE | cut -d: -f1) \
&& sed -i "$N i fips.provider.1=OpenSSL" $JAVA_SECURITY_POLICY_FILE

RUN for I in {20..1}; do J=$((I+1)); sed -i "s/security.provider.$I=/security.provider.$J=/" $JAVA_SECURITY_POLICY_FILE; done \
&& N=$(grep -m 1 -n "^security\.provider\.[[:digit:]]=" $JAVA_SECURITY_POLICY_FILE | cut -d: -f1) \
&& sed -i "$N i security.provider.1=OpenSSL" $JAVA_SECURITY_POLICY_FILE

#Set PKCS11 as keystore type for non FIPS
RUN sed -i -e "s/^keystore.type=.*/keystore.type=PKCS11/g" \
-e "s/\(^security\.provider\.[0-9]*=SunPKCS11$\)/\1 \$\{java\.home\}\/conf\/security\/nss.cfg/" $JAVA_SECURITY_POLICY_FILE

#Set NSS DB configs for non FIPS
RUN sed -i -e "s/^nssDbMode =.*/nssDbMode = readOnly/g" \
-e '/^nssDbMode =.*/a nssSecmodDirectory = sql:/etc/pki/nssdb\nnssModule = keystore' \
-e 's/handleStartupErrors = ignoreMultipleInitialisation$//g' \
$JAVA_HOME/conf/security/nss.cfg

ENTRYPOINT ["/start_freeipa_app.sh"]
