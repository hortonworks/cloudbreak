FROM docker-private.infra.cloudera.com/cloudera_base/ubi8/cldr-openjdk-11-runtime-cis:1.15-1.1679485252-cis-27032023
MAINTAINER info@cloudera.com

# REPO URL to download jar
ARG REPO_URL=https://repo.hortonworks.com/content/repositories/releases
ARG VERSION=''

ENV VERSION ${VERSION}

WORKDIR /

# install the cloudbreak app
ADD ${REPO_URL}/com/sequenceiq/mock-infrastructure/$VERSION/mock-infrastructure-$VERSION.jar /mock-infrastructure.jar

CMD (java -jar /mock-infrastructure.jar) & JAVAPID=$!; trap "kill $JAVAPID; wait $JAVAPID" INT TERM; wait $JAVAPID