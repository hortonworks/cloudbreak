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

# install the environment app
ADD ${REPO_URL}/com/sequenceiq/environment/$VERSION/environment-$VERSION.jar /environment.jar

# extract schema files
RUN ( unzip environment.jar schema/* -d / ) || \
    ( unzip environment.jar BOOT-INF/classes/schema/* -d /tmp/ && mv /tmp/BOOT-INF/classes/schema/ /schema/ )

# Install starter script for the Environment application
COPY bootstrap/start_environment_app.sh /
COPY bootstrap/wait_for_environment_api.sh /

ENTRYPOINT ["/start_environment_app.sh"]