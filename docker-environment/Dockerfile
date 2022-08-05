FROM docker-private.infra.cloudera.com/cloudera_base/ubi8/cldr-ubi-minimal-java:jdk11-ubi8.5-218
# We can not use alpine based image because of https://github.com/grpc/grpc-java/issues/8751
MAINTAINER info@cloudera.com

# REPO URL to download jar
ARG REPO_URL=https://repo.hortonworks.com/content/repositories/releases
ARG VERSION=''

ENV VERSION ${VERSION}

WORKDIR /

RUN microdnf install unzip

# install the environment app
ADD ${REPO_URL}/com/sequenceiq/environment/$VERSION/environment-$VERSION.jar /environment.jar

# add jmx exporter
ADD jmx_prometheus_javaagent-0.16.1.jar /jmx_prometheus_javaagent.jar

# extract schema files
RUN ( unzip environment.jar schema/* -d / ) || \
    ( unzip environment.jar BOOT-INF/classes/schema/* -d /tmp/ && mv /tmp/BOOT-INF/classes/schema/ /schema/ )

# Install starter script for the Environment application
COPY bootstrap/start_environment_app.sh /
COPY bootstrap/wait_for_environment_api.sh /

ENTRYPOINT ["/start_environment_app.sh"]
