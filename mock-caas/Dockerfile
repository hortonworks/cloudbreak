FROM hortonworks/hwx_openjdk:11.0-jdk-slim
MAINTAINER info@hortonworks.com

# REPO URL to download jar
ARG REPO_URL=https://repo.hortonworks.com/content/repositories/releases
ARG VERSION=''

ENV VERSION ${VERSION}

WORKDIR /

# install the cloudbreak app
ADD ${REPO_URL}/com/sequenceiq/mock-caas/$VERSION/mock-caas-$VERSION.jar /mock-caas.jar

CMD (java -jar /mock-caas.jar) & JAVAPID=$!; trap "kill $JAVAPID; wait $JAVAPID" INT TERM; wait $JAVAPID