FROM openjdk:11-jdk-slim
MAINTAINER info@hortonworks.com

WORKDIR /

# Install zip
RUN apt-get update --no-install-recommends && apt-get install -y zip procps wget && apt-get clean && rm -rf /var/lib/apt/lists/*

ADD cloudbreak.jar /
ADD start_cloudbreak_app.sh /
ADD wait_for_cloudbreak_api.sh /
ADD jmx_prometheus_javaagent-0.10.jar /

# extract schema files
RUN unzip -o cloudbreak.jar BOOT-INF/classes/schema/* -d /tmp/ \
    && mv /tmp/BOOT-INF/classes/schema/ /schema/

ENTRYPOINT ["/start_cloudbreak_app.sh"]
