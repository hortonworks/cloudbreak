FROM docker-private.infra.cloudera.com/cloudera_base/hardened/cloudera-openjdk:jdk-21-fips
MAINTAINER info@cloudera.com

# REPO URL to download jar
ARG REPO_URL=https://nexus-private.eng.cloudera.com/nexus/content/groups/public
ARG VERSION=''

ENV VERSION ${VERSION}

WORKDIR /

USER root

# install the cloudbreak app
ADD ${REPO_URL}/com/sequenceiq/mock-thunderhead/$VERSION/mock-thunderhead-$VERSION.jar /mock-thunderhead.jar
RUN chmod 644 /mock-thunderhead.jar

CMD (java -Djava.security.properties=/usr/lib/jvm/jdk-fips-config/unapproved.java.security -jar /mock-thunderhead.jar) & JAVAPID=$!; trap "kill $JAVAPID; wait $JAVAPID" INT TERM; wait $JAVAPID
