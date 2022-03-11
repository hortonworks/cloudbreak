FROM docker-private.infra.cloudera.com/cloudera_base/cldr-java:11.0.13-cldr-jre-slim-buster-15122021
MAINTAINER info@cloudera.com

WORKDIR /

ADD cloudbreak.jar /
ADD start_cloudbreak_app.sh /
ADD wait_for_cloudbreak_api.sh /
ADD jmx_prometheus_javaagent-0.10.jar /

# extract schema files
# extract schema files
RUN ( unzip cloudbreak.jar schema/* -d / ) || \
    ( unzip cloudbreak.jar BOOT-INF/classes/schema/* -d /tmp/ && mv /tmp/BOOT-INF/classes/schema/ /schema/ )

ENTRYPOINT ["/start_cloudbreak_app.sh"]
