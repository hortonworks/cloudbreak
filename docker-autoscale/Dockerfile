FROM docker-private.infra.cloudera.com/cloudera_base/cldr-java:11.0.13-cldr-jre-slim-buster-15122021
# We can not use alpine based image because of https://github.com/grpc/grpc-java/issues/8751
MAINTAINER info@cloudera.com

# REPO URL to download jar
ARG REPO_URL=http://repo.hortonworks.com/content/repositories/releases
ARG VERSION=''

ENV VERSION ${VERSION}

WORKDIR /

RUN apt-get install unzip

# install the periscope app
ADD ${REPO_URL}/com/sequenceiq/periscope/$VERSION/periscope-$VERSION.jar /periscope.jar

# add jmx exporter
ADD jmx_prometheus_javaagent-0.16.1.jar /jmx_prometheus_javaagent.jar

# extract schema files
RUN ( unzip periscope.jar schema/* -d / ) || \
    ( unzip periscope.jar BOOT-INF/classes/schema/* -d /tmp/ && mv /tmp/BOOT-INF/classes/schema/ /schema/ )

# Install starter script for the Autoscale application
COPY bootstrap/start_autoscale_app.sh /
COPY bootstrap/wait_for_autoscale_api.sh /

ENTRYPOINT ["/start_autoscale_app.sh"]
