FROM docker-private.infra.cloudera.com/cloudera_base/openjdk:11.0.6-jdk-slim-buster
MAINTAINER info@hortonworks.com

WORKDIR /

ADD cloudbreak.jar /
ADD start_cloudbreak_app.sh /
ADD wait_for_cloudbreak_api.sh /
ADD jmx_prometheus_javaagent-0.10.jar /

# extract schema files
RUN jar xf cloudbreak.jar && mv /BOOT-INF/classes/schema/ /schema/

ENTRYPOINT ["/start_cloudbreak_app.sh"]
