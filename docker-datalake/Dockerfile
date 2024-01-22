FROM docker-private.infra.cloudera.com/cloudera_base/ubi8/cldr-openjdk-17-runtime-cis:1.18-2-stig-18012024
# We can not use alpine based image because of https://github.com/grpc/grpc-java/issues/8751
MAINTAINER info@cloudera.com

# REPO URL to download jar
ARG REPO_URL=https://nexus-private.hortonworks.com/nexus/content/groups/public
ARG VERSION=''
ARG TARGETARCH=''

ENV VERSION ${VERSION}

WORKDIR /

RUN microdnf install unzip

# install the datalake app
ADD ${REPO_URL}/com/sequenceiq/datalake/$VERSION/datalake-$VERSION.jar /datalake.jar

# extract schema files
RUN ( unzip datalake.jar schema/* -d / ) || \
    ( unzip datalake.jar BOOT-INF/classes/schema/* -d /tmp/ && mv /tmp/BOOT-INF/classes/schema/ /schema/ )

# Install starter script for the Datalake application
COPY bootstrap/start_datalake_app.sh /
COPY bootstrap/wait_for_datalake_api.sh /

ENV JAVA_SECURITY_POLICY_FILE $JAVA_HOME/conf/security/java.security

# Add OpenSSL provider to Java security policy file (FIPS enabled mode only).
# - insert a line: fips.provider.1=OpenSSL
# - increase the index for other fips.provider lines.
RUN if [[ "${TARGETARCH}" != "arm64" ]]; then \
      for I in {20..1}; do J=$((I+1)); sed -i "s/fips.provider.$I=/fips.provider.$J=/" $JAVA_SECURITY_POLICY_FILE; done \
      && N=$(grep -m 1 -n "^fips\.provider\.[[:digit:]]=" $JAVA_SECURITY_POLICY_FILE | cut -d: -f1) \
      && sed -i "$N i fips.provider.1=OpenSSL" $JAVA_SECURITY_POLICY_FILE ; \
    fi

RUN if [[ "${TARGETARCH}" != "arm64" ]]; then \
      for I in {20..1}; do J=$((I+1)); sed -i "s/security.provider.$I=/security.provider.$J=/" $JAVA_SECURITY_POLICY_FILE; done \
      && N=$(grep -m 1 -n "^security\.provider\.[[:digit:]]=" $JAVA_SECURITY_POLICY_FILE | cut -d: -f1) \
      && sed -i "$N i security.provider.1=OpenSSL" $JAVA_SECURITY_POLICY_FILE ; \
    fi

#Set PKCS11 as keystore type for non FIPS
RUN sed -i -e "s/^keystore.type=.*/keystore.type=PKCS11/g" \
-e "s/\(^security\.provider\.[0-9]*=SunPKCS11$\)/\1 \$\{java\.home\}\/conf\/security\/nss.cfg/" $JAVA_SECURITY_POLICY_FILE

#Set NSS DB configs for non FIPS
RUN sed -i -e "s/^nssDbMode =.*/nssDbMode = readOnly/g" \
-e '/^nssDbMode =.*/a nssSecmodDirectory = sql:/etc/pki/nssdb\nnssModule = keystore' \
-e 's/handleStartupErrors = ignoreMultipleInitialisation$//g' \
$JAVA_HOME/conf/security/nss.cfg

ENTRYPOINT ["/start_datalake_app.sh"]
