FROM docker-private.infra.cloudera.com/cloudera_base/hardened/cloudera-openjdk:jdk-21-fips
# We can not use alpine based image because of https://github.com/grpc/grpc-java/issues/8751
LABEL maintainer="info@cloudera.com"

# REPO URL to download jar
ARG REPO_URL=https://nexus-private.eng.cloudera.com/nexus/content/groups/public
ARG VERSION=''
ARG TARGETARCH=''
ARG FIPS_MODE_ENABLED=true

ENV FIPS_MODE_ENABLED=${FIPS_MODE_ENABLED}
ENV VERSION=${VERSION}

WORKDIR /

USER root
RUN apk add --no-cache unzip

# install the Maintenance app
ADD ${REPO_URL}/com/sequenceiq/maintenance/$VERSION/maintenance-$VERSION.jar /maintenance.jar
RUN chmod 644 /maintenance.jar

# extract schema files
RUN ( unzip maintenance.jar schema/* -d / ) || \
    ( unzip maintenance.jar BOOT-INF/classes/schema/* -d /tmp/ && mv /tmp/BOOT-INF/classes/schema/ /schema/ )

# Install starter script for the Maintenance application
COPY bootstrap/start_maintenance_app.sh /
COPY bootstrap/wait_for_maintenance_api.sh /
COPY bootstrap/ImportCerts.java /

RUN chmod +x /start_maintenance_app.sh /wait_for_maintenance_api.sh

WORKDIR /tmp

ENTRYPOINT ["/start_maintenance_app.sh"]
