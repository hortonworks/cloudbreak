FROM hortonworks/hwx_openjdk:11-jdk-slim
MAINTAINER info@hortonworks.com

# REPO URL to download jar
ARG REPO_URL=http://repo.hortonworks.com/content/repositories/releases
ARG VERSION=''

ENV VERSION ${VERSION}

WORKDIR /

# Install zip
RUN apt-get update --no-install-recommends && apt-get install -y zip procps && apt-get clean && rm -rf /var/lib/apt/lists/*

# Install zip
RUN apt-get update --no-install-recommends && apt-get install -y zip procps

# install the periscope app
ADD ${REPO_URL}/com/sequenceiq/periscope/$VERSION/periscope-$VERSION.jar /periscope.jar

# add jmx exporter
ADD jmx_prometheus_javaagent-0.10.jar /jmx_prometheus_javaagent.jar

# extract schema files
RUN ( unzip periscope.jar schema/* -d / ) || \
    ( unzip periscope.jar BOOT-INF/classes/schema/* -d /tmp/ && mv /tmp/BOOT-INF/classes/schema/ /schema/ )

# Install starter script for the Autoscale application
COPY bootstrap/start_autoscale_app.sh /
COPY bootstrap/wait_for_autoscale_api.sh /

ENTRYPOINT ["/start_autoscale_app.sh"]
