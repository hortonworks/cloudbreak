FROM docker-private.infra.cloudera.com/cloudera_base/ubi8/cldr-openjdk-11-runtime:1.14-3-03082022
# We can not use alpine based image because of https://github.com/grpc/grpc-java/issues/8751
MAINTAINER info@cloudera.com

# REPO URL to download jar
ARG REPO_URL=https://repo.hortonworks.com/content/repositories/releases
ARG VERSION=''

ENV VERSION ${VERSION}
ENV PROMETHEUS_VERSION 0.17.2

WORKDIR /

RUN microdnf install unzip

# install the environment app
ADD ${REPO_URL}/com/sequenceiq/environment/$VERSION/environment-$VERSION.jar /environment.jar

# add jmx exporter
RUN curl -o /jmx_prometheus_javaagent.jar https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/$PROMETHEUS_VERSION/jmx_prometheus_javaagent-$PROMETHEUS_VERSION.jar

# extract schema files
RUN ( unzip environment.jar schema/* -d / ) || \
    ( unzip environment.jar BOOT-INF/classes/schema/* -d /tmp/ && mv /tmp/BOOT-INF/classes/schema/ /schema/ )

# Install starter script for the Environment application
COPY bootstrap/start_environment_app.sh /
COPY bootstrap/wait_for_environment_api.sh /

ENTRYPOINT ["/start_environment_app.sh"]
