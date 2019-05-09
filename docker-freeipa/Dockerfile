FROM hortonworks/hwx_openjdk:11.0-jdk-slim
MAINTAINER info@hortonworks.com

# REPO URL to download jar
ARG REPO_URL=https://repo.hortonworks.com/content/repositories/releases
ARG VERSION=''

ENV VERSION ${VERSION}

WORKDIR /

# Install zip
RUN apt-get update --no-install-recommends && apt-get install -y zip procps && apt-get clean && rm -rf /var/lib/apt/lists/*

# Install zip
RUN apt-get update --no-install-recommends && apt-get install -y zip procps

# install the freeipa app
ADD ${REPO_URL}/com/sequenceiq/freeipa/$VERSION/freeipa-$VERSION.jar /freeipa.jar

# add jmx exporter
ADD jmx_prometheus_javaagent-0.10.jar /jmx_prometheus_javaagent.jar

# extract schema files
RUN ( unzip freeipa.jar schema/* -d / ) || \
    ( unzip freeipa.jar BOOT-INF/classes/schema/* -d /tmp/ && mv /tmp/BOOT-INF/classes/schema/ /schema/ )

# Install starter script for the FreeIpa application
COPY bootstrap/start_freeipa_app.sh /
COPY bootstrap/wait_for_freeipa_api.sh /

ENTRYPOINT ["/start_freeipa_app.sh"]
