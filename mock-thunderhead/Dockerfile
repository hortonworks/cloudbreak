FROM docker-private.infra.cloudera.com/cloudera_base/ubi8/cldr-openjdk-21-runtime-cis:1.20-2.1724181173-stig-09092024
MAINTAINER info@cloudera.com

# REPO URL to download jar
ARG REPO_URL=https://nexus-private.eng.cloudera.com/nexus/content/groups/public
ARG VERSION=''

ENV VERSION ${VERSION}

WORKDIR /

# install the cloudbreak app
ADD ${REPO_URL}/com/sequenceiq/mock-thunderhead/$VERSION/mock-thunderhead-$VERSION.jar /mock-thunderhead.jar

CMD (java -jar /mock-thunderhead.jar) & JAVAPID=$!; trap "kill $JAVAPID; wait $JAVAPID" INT TERM; wait $JAVAPID
