FROM docker-private.infra.cloudera.com/cloudera_base/cldr-java:11.0.13-cldr-jre-slim-buster-15122021
# We can not use alpine based image because of https://github.com/grpc/grpc-java/issues/8751
MAINTAINER info@cloudera.com

# REPO URL to download jar
ARG REPO_URL=https://repo.hortonworks.com/content/repositories/releases
ARG VERSION=''

ENV VERSION ${VERSION}

WORKDIR /

RUN apt-get install unzip

# install the Redbeams app
ADD ${REPO_URL}/com/sequenceiq/redbeams/$VERSION/redbeams-$VERSION.jar /redbeams.jar

# add jmx exporter
ADD jmx_prometheus_javaagent-0.16.1.jar /jmx_prometheus_javaagent.jar

# extract schema files
RUN ( unzip redbeams.jar schema/* -d / ) || \
    ( unzip redbeams.jar BOOT-INF/classes/schema/* -d /tmp/ && mv /tmp/BOOT-INF/classes/schema/ /schema/ )

# Install starter script for the Redbeams application
COPY bootstrap/start_redbeams_app.sh /
COPY bootstrap/wait_for_redbeams_api.sh /

ENTRYPOINT ["/start_redbeams_app.sh"]
