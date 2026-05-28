FROM docker-private.infra.cloudera.com/cloudera_base/hardened/cloudera-openjdk:jdk-21-fips
# We can not use alpine based image because of https://github.com/grpc/grpc-java/issues/8751
MAINTAINER info@cloudera.com

# REPO URL to download jar
ARG REPO_URL=https://nexus-private.eng.cloudera.com/nexus/content/groups/public
ARG VERSION=''
ARG TARGETARCH=''
ARG FIPS_MODE_ENABLED=false

ENV FIPS_MODE_ENABLED=${FIPS_MODE_ENABLED}
ENV VERSION=${VERSION}
ENV PROMETHEUS_VERSION=0.17.2

WORKDIR /

USER root
RUN apk add --no-cache unzip

# install the remote environment app
ADD ${REPO_URL}/com/sequenceiq/environment-remote/$VERSION/environment-remote-$VERSION.jar /remote-environment.jar

# extract schema files
RUN ( unzip remote-environment.jar schema/* -d / ) || \
    ( unzip remote-environment.jar BOOT-INF/classes/schema/* -d /tmp/ && mv /tmp/BOOT-INF/classes/schema/ /schema/ )

# Install starter script for the Remote Environment application
COPY bootstrap/start_remoteenvironment_app.sh /

ENTRYPOINT ["/start_remoteenvironment_app.sh"]
