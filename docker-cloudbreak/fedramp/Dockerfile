FROM docker-private.infra.cloudera.com/cloudera_base/hardened/cloudera-openjdk:jdk-21-fips
# We can not use alpine based image because of https://github.com/grpc/grpc-java/issues/8751
MAINTAINER info@cloudera.com

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

# install the cloudbreak app
ADD ${REPO_URL}/com/sequenceiq/cloudbreak/$VERSION/cloudbreak-$VERSION.jar /cloudbreak.jar

# extract schema files
RUN ( unzip cloudbreak.jar schema/* -d / ) || \
    ( unzip cloudbreak.jar BOOT-INF/classes/schema/* -d /tmp/ && mv /tmp/BOOT-INF/classes/schema/ /schema/ )

# Install starter script for the Cloudbreak application
COPY bootstrap/start_cloudbreak_app.sh /
COPY bootstrap/wait_for_cloudbreak_api.sh /

ENTRYPOINT ["/start_cloudbreak_app.sh"]
