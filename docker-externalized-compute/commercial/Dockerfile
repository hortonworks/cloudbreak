FROM docker-private.infra.cloudera.com/cloudera_base/hardened/cloudera-openjdk:jdk-21-fips
# We can not use alpine based image because of https://github.com/grpc/grpc-java/issues/8751
MAINTAINER info@cloudera.com

# REPO URL to download jar
ARG REPO_URL=https://nexus-private.hortonworks.com/nexus/content/groups/public
ARG VERSION=''
ARG TARGETARCH=''
ARG FIPS_MODE_ENABLED=false

ENV FIPS_MODE_ENABLED=${FIPS_MODE_ENABLED}
ENV VERSION=${VERSION}
ENV PROMETHEUS_VERSION=0.17.2

WORKDIR /

USER root
RUN apk add --no-cache unzip

# install the externalized compute app
ADD ${REPO_URL}/com/sequenceiq/externalized-compute/$VERSION/externalized-compute-$VERSION.jar /externalized-compute.jar

# extract schema files
RUN ( unzip externalized-compute.jar schema/* -d / ) || \
    ( unzip externalized-compute.jar BOOT-INF/classes/schema/* -d /tmp/ && mv /tmp/BOOT-INF/classes/schema/ /schema/ )

# Install starter script for the Externalized Compute application
COPY bootstrap/start_externalizedcompute_app.sh /
COPY bootstrap/wait_for_externalizedcompute_api.sh /

ENTRYPOINT ["/start_externalizedcompute_app.sh"]
