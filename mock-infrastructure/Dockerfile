FROM docker-private.infra.cloudera.com/cloudera_base/hardened/cloudera-openjdk:jdk-21-fips
MAINTAINER info@cloudera.com

# REPO URL to download jar
ARG REPO_URL=https://nexus-private.eng.cloudera.com/nexus/content/groups/public
ARG VERSION=''

ENV VERSION ${VERSION}

WORKDIR /

USER root

# install the cloudbreak app
ADD ${REPO_URL}/com/sequenceiq/mock-infrastructure/$VERSION/mock-infrastructure-$VERSION.jar /mock-infrastructure.jar
RUN chmod 644 /mock-infrastructure.jar

CMD (java --add-opens java.base/java.util=ALL-UNNAMED -Djava.security.properties=/usr/lib/jvm/jdk-fips-config/unapproved.java.security -jar /mock-infrastructure.jar) & JAVAPID=$!; trap "kill $JAVAPID; wait $JAVAPID" INT TERM; wait $JAVAPID
