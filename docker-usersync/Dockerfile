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

# install the Usersync app
ADD ${REPO_URL}/com/sequenceiq/usersync/$VERSION/usersync-$VERSION.jar /usersync.jar

# extract schema files
RUN ( unzip usersync.jar schema/* -d / ) || \
    ( unzip usersync.jar BOOT-INF/classes/schema/* -d /tmp/ && mv /tmp/BOOT-INF/classes/schema/ /schema/ )

# Install starter script for the Usersync application
COPY bootstrap/start_usersync_app.sh /
COPY bootstrap/wait_for_usersync_api.sh /

ENTRYPOINT ["/start_usersync_app.sh"]
