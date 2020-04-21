FROM docker-private.infra.cloudera.com/cloudera_base/cldr-java:11.0.7-cldr1-jdk-slim-buster
MAINTAINER info@hortonworks.com

# REPO URL to download jar
ARG REPO_URL=http://repo.hortonworks.com/content/repositories/releases
ARG VERSION=''

ENV VERSION ${VERSION}

WORKDIR /

# install the periscope app
ADD ${REPO_URL}/com/sequenceiq/periscope/$VERSION/periscope-$VERSION.jar /periscope.jar

# add jmx exporter
ADD jmx_prometheus_javaagent-0.10.jar /jmx_prometheus_javaagent.jar

# extract schema files
RUN jar xf periscope.jar && mv /BOOT-INF/classes/schema/ /schema/

# Install starter script for the Autoscale application
COPY bootstrap/start_autoscale_app.sh /
COPY bootstrap/wait_for_autoscale_api.sh /

ENTRYPOINT ["/start_autoscale_app.sh"]
