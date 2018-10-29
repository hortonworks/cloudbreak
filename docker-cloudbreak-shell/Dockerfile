FROM hortonworks/hwx_openjdk:8-jdk-slim
MAINTAINER info@hortonworks.com

# Install zip, bash
RUN apt-get update --no-install-recommends && apt-get install -y zip bash

# REPO URL to download jar
ARG REPO_URL=https://repo.hortonworks.com/content/repositories/releases
ARG VERSION=''

ENV VERSION ${VERSION}

# install the cloudbreak app
ADD ${REPO_URL}/com/sequenceiq/cloudbreak-shell/$VERSION/cloudbreak-shell-$VERSION.jar /cloudbreak-shell.jar

COPY logback.xml /logback.xml

ADD start /start
ENTRYPOINT [ "/start" ]