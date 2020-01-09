FROM docker-private.infra.cloudera.com/cloudera_base/cldr-java:11.0.5-cldr1-jdk-slim-buster
MAINTAINER info@hortonworks.com

# REPO URL to download jar
ARG REPO_URL=https://repo.hortonworks.com/content/repositories/releases
ARG VERSION=''

ENV VERSION ${VERSION}

WORKDIR /

# install the Redbeams app
ADD ${REPO_URL}/com/sequenceiq/redbeams/$VERSION/redbeams-$VERSION.jar /redbeams.jar

# add jmx exporter
ADD jmx_prometheus_javaagent-0.10.jar /jmx_prometheus_javaagent.jar

# extract schema files
RUN jar xf redbeams.jar && mv /BOOT-INF/classes/schema/ /schema/

# Install starter script for the Redbeams application
COPY bootstrap/start_redbeams_app.sh /
COPY bootstrap/wait_for_redbeams_api.sh /

ENTRYPOINT ["/start_redbeams_app.sh"]
