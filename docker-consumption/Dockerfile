FROM docker-private.infra.cloudera.com/cloudera_base/ubi8/cldr-openjdk-11-runtime:1.14-5-04102022
# We can not use alpine based image because of https://github.com/grpc/grpc-java/issues/8751
MAINTAINER info@cloudera.com

# REPO URL to download jar
ARG REPO_URL=https://repo.hortonworks.com/content/repositories/releases
ARG VERSION=''

ENV VERSION ${VERSION}

WORKDIR /

RUN microdnf install unzip

# install the consumption app
ADD ${REPO_URL}/com/sequenceiq/cloud-consumption/$VERSION/cloud-consumption-$VERSION.jar /consumption.jar

# add jmx exporter
ADD jmx_prometheus_javaagent-0.17.2.jar /jmx_prometheus_javaagent.jar

# extract schema files
RUN ( unzip consumption.jar schema/* -d / ) || \
    ( unzip consumption.jar BOOT-INF/classes/schema/* -d /tmp/ && mv /tmp/BOOT-INF/classes/schema/ /schema/ )

# Install starter script for the Consumption application
COPY bootstrap/start_consumption_app.sh /
COPY bootstrap/wait_for_consumption_api.sh /

ENTRYPOINT ["/start_consumption_app.sh"]
