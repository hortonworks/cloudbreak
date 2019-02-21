FROM openjdk:11-jdk-slim
MAINTAINER info@hortonworks.com

WORKDIR /

# Install zip
RUN apt-get update --no-install-recommends && apt-get install -y zip procps wget && apt-get clean && rm -rf /var/lib/apt/lists/*

ADD cloudbreak.jar /

# extract schema files
RUN unzip -o cloudbreak.jar BOOT-INF/classes/schema/* -d /tmp/ \
    && mv /tmp/BOOT-INF/classes/schema/ /schema/

# Install starter script for the Cloudbreak application
RUN wget https://raw.githubusercontent.com/hortonworks/docker-cloudbreak/master/bootstrap/start_cloudbreak_app.sh && \
    chmod +x /start_cloudbreak_app.sh
RUN wget https://raw.githubusercontent.com/hortonworks/docker-cloudbreak/master/bootstrap/wait_for_cloudbreak_api.sh && \
    chmod +x /wait_for_cloudbreak_api.sh
RUN wget https://raw.githubusercontent.com/hortonworks/docker-cloudbreak/master/jmx_prometheus_javaagent-0.10.jar

ENTRYPOINT ["/start_cloudbreak_app.sh"]
