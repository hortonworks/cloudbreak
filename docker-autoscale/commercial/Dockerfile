FROM docker-private.infra.cloudera.com/cloudera_base/hardened/cloudera-openjdk:jdk-21-runtime-nofips
# We can not use alpine based image because of https://github.com/grpc/grpc-java/issues/8751
MAINTAINER info@cloudera.com

# REPO URL to download jar
ARG REPO_URL=https://nexus-private.hortonworks.com/nexus/content/groups/public
ARG VERSION=''
ARG TARGETARCH=''

ENV VERSION ${VERSION}

WORKDIR /

USER root
RUN apk add --no-cache unzip libnss

# install the periscope app
ADD ${REPO_URL}/com/sequenceiq/periscope/$VERSION/periscope-$VERSION.jar /periscope.jar

# extract schema files
RUN ( unzip periscope.jar schema/* -d / ) || \
    ( unzip periscope.jar BOOT-INF/classes/schema/* -d /tmp/ && mv /tmp/BOOT-INF/classes/schema/ /schema/ )

# Install starter script for the Autoscale application
COPY bootstrap/start_autoscale_app.sh /
COPY bootstrap/wait_for_autoscale_api.sh /

# Copy NSS DB config for non-FIPS
COPY bootstrap/nss.cfg $JAVA_HOME/conf/security/nss.cfg

ENV JAVA_SECURITY_POLICY_FILE $JAVA_HOME/conf/security/java.security

# Add OpenSSL provider to Java security policy file (FIPS enabled mode only).
# - insert a line: fips.provider.1=OpenSSL
# - increase the index for other fips.provider lines.
RUN if [[ "${TARGETARCH}" != "arm64" ]]; then \
      for I in $(seq 20 -1 1); do J=$((I+1)); sed -i "s/fips.provider.$I=/fips.provider.$J=/" $JAVA_SECURITY_POLICY_FILE; done \
      && N=$(grep -m 1 -n "^fips\.provider\.[[:digit:]]=" $JAVA_SECURITY_POLICY_FILE | cut -d: -f1) \
      && if [ -n "$N" ]; then \
             sed -i "$N i fips.provider.1=OpenSSL" $JAVA_SECURITY_POLICY_FILE; \
         fi \
    fi

RUN if [[ "${TARGETARCH}" != "arm64" ]]; then \
      for I in $(seq 20 -1 1); do J=$((I+1)); sed -i "s/security.provider.$I=/security.provider.$J=/" $JAVA_SECURITY_POLICY_FILE; done \
      && N=$(grep -m 1 -n "^security\.provider\.[[:digit:]]=" $JAVA_SECURITY_POLICY_FILE | cut -d: -f1) \
      && sed -i "$N i security.provider.1=OpenSSL" $JAVA_SECURITY_POLICY_FILE ; \
    fi

#Set PKCS11 as keystore type for non FIPS
RUN sed -i -e "s/^keystore.type=.*/keystore.type=PKCS11/g" \
-e "s/\(^security\.provider\.[0-9]*=SunPKCS11$\)/\1 \$\{java\.home\}\/conf\/security\/nss.cfg/" $JAVA_SECURITY_POLICY_FILE

ENTRYPOINT ["/start_autoscale_app.sh"]
