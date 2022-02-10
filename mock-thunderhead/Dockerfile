FROM docker-private.infra.cloudera.com/cloudera_base/ubi8/cldr-ubi-minimal-java:jdk11-ubi8.5-218
MAINTAINER info@cloudera.com

# REPO URL to download jar
ARG REPO_URL=https://repo.hortonworks.com/content/repositories/releases
ARG VERSION=''

ENV VERSION ${VERSION}

WORKDIR /

# install the cloudbreak app
ADD ${REPO_URL}/com/sequenceiq/mock-thunderhead/$VERSION/mock-thunderhead-$VERSION.jar /mock-thunderhead.jar

CMD (java -jar /mock-thunderhead.jar) & JAVAPID=$!; trap "kill $JAVAPID; wait $JAVAPID" INT TERM; wait $JAVAPID