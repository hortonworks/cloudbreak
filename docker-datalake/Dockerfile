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

# install the datalake app
ADD ${REPO_URL}/com/sequenceiq/datalake/$VERSION/datalake-$VERSION.jar /datalake.jar

# extract schema files
RUN ( unzip datalake.jar schema/* -d / ) || \
    ( unzip datalake.jar BOOT-INF/classes/schema/* -d /tmp/ && mv /tmp/BOOT-INF/classes/schema/ /schema/ )

# Install starter script for the Datalake application
COPY bootstrap/start_datalake_app.sh /
COPY bootstrap/wait_for_datalake_api.sh /

ENTRYPOINT ["/start_datalake_app.sh"]