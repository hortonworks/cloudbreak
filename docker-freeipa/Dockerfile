FROM docker-private.infra.cloudera.com/cloudera_base/ubi8/cldr-openjdk-11-runtime:1.14-6.1666624577-02112022
# We can not use alpine based image because of https://github.com/grpc/grpc-java/issues/8751
MAINTAINER info@cloudera.com

# REPO URL to download jar
ARG REPO_URL=https://repo.hortonworks.com/content/repositories/releases
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

ENTRYPOINT ["/start_freeipa_app.sh"]
