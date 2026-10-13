FROM docker-private.infra.cloudera.com/cloudera_base/hardened/cloudera-openjdk:jdk-25-runtime-nofips
MAINTAINER info@cloudera.com

WORKDIR /

ADD cloudbreak.jar /
ADD start_cloudbreak_app.sh /
ADD wait_for_cloudbreak_api.sh /
ADD jmx_prometheus_javaagent-0.10.jar /

# Root is needed to write /schema; the preinstalled unzip is busybox, which exits 0 on a
# non-matching pattern and would skip the fallback below, leaving the image without /schema.
USER root
RUN apk add --no-cache unzip

# extract schema files
RUN ( unzip cloudbreak.jar schema/* -d / ) || \
    ( unzip cloudbreak.jar BOOT-INF/classes/schema/* -d /tmp/ && mv /tmp/BOOT-INF/classes/schema/ /schema/ )

ENTRYPOINT ["/start_cloudbreak_app.sh"]
