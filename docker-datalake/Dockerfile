FROM docker-private.infra.cloudera.com/cloudera_base/cldr-java:11.0.5-cldr1-jdk-slim-buster
MAINTAINER info@hortonworks.com

# REPO URL to download jar
ARG REPO_URL=https://repo.hortonworks.com/content/repositories/releases
ARG VERSION=''

ENV VERSION ${VERSION}

WORKDIR /

# install the datalake app
ADD ${REPO_URL}/com/sequenceiq/datalake/$VERSION/datalake-$VERSION.jar /datalake.jar

# add jmx exporter
ADD jmx_prometheus_javaagent-0.10.jar /jmx_prometheus_javaagent.jar

# extract schema files
RUN jar xf datalake.jar && mv /BOOT-INF/classes/schema/ /schema/

# Install starter script for the Datalake application
COPY bootstrap/start_datalake_app.sh /
COPY bootstrap/wait_for_datalake_api.sh /

ENTRYPOINT ["/start_datalake_app.sh"]
