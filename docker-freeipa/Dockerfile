FROM docker-private.infra.cloudera.com/cloudera_base/cldr-java:11.0.6-cldr1-jdk-slim-buster
MAINTAINER info@hortonworks.com

# REPO URL to download jar
ARG REPO_URL=https://repo.hortonworks.com/content/repositories/releases
ARG VERSION=''

ENV VERSION ${VERSION}

WORKDIR /

# install the freeipa app
ADD ${REPO_URL}/com/sequenceiq/freeipa/$VERSION/freeipa-$VERSION.jar /freeipa.jar

# add jmx exporter
ADD jmx_prometheus_javaagent-0.10.jar /jmx_prometheus_javaagent.jar

# extract schema files
RUN jar xf freeipa.jar && mv /BOOT-INF/classes/schema/ /schema/

# Install starter script for the FreeIpa application
COPY bootstrap/start_freeipa_app.sh /
COPY bootstrap/wait_for_freeipa_api.sh /

ENTRYPOINT ["/start_freeipa_app.sh"]
